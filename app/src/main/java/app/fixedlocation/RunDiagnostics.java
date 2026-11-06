package app.fixedlocation;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import java.text.SimpleDateFormat;
import java.util.*;

/** On-device diagnostics only. No uploads, no raw observed coordinates in reports. */
final class RunDiagnostics {
    private RunDiagnostics() {}

    static String readbacks(Context context, LocationManager manager, List<String> providers, Coordinates target) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return "系统缓存回读：缺少精确位置权限，无法可靠比较。请在应用权限设置中开启精确位置。";
        }
        StringBuilder result = new StringBuilder("系统缓存回读（不代表百度/微信采纳）：");
        for (String provider : providers) {
            result.append('\n').append(provider).append("：");
            try {
                Location location = manager.getLastKnownLocation(provider);
                if (location == null) { result.append("暂无缓存"); continue; }
                long age = (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1000000;
                float[] distance = new float[1];
                Location.distanceBetween(target.latitude, target.longitude, location.getLatitude(), location.getLongitude(), distance);
                UpdateHealth.Readback status = UpdateHealth.readback(age, distance[0], location.isMock());
                switch (status) {
                    case MATCH: result.append("模拟位置与目标相符"); break;
                    case STALE: result.append("缓存过期，不能确认持续更新"); break;
                    case NON_MOCK: result.append("收到非模拟位置，需排查来源"); break;
                    case DIFFERENT: result.append("模拟位置偏离目标，可能存在冲突"); break;
                    default: result.append("缓存时间异常"); break;
                }
                result.append(String.format(Locale.CHINA, " · %.1f秒前 · mock=%s", age / 1000d, location.isMock()));
            } catch (RuntimeException error) {
                result.append("回读失败：").append(error.getClass().getSimpleName());
            }
        }
        return result.toString();
    }

    static void save(Context context, String message) {
        context.getSharedPreferences("diagnostics", Context.MODE_PRIVATE).edit()
                .putString("last", stamp() + "\n" + message).apply();
    }

    static String previous(Context context) {
        return context.getSharedPreferences("diagnostics", Context.MODE_PRIVATE)
                .getString("last", "暂无运行记录");
    }

    static String report(Context context, String current) {
        PowerManager power = context.getSystemService(PowerManager.class);
        String version = "未知";
        try { version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; }
        catch (PackageManager.NameNotFoundException ignored) {}
        String result = "定点诊断 " + version + "\n" + stamp()
                + "\n设备：" + Build.MANUFACTURER + " " + Build.MODEL
                + "\n系统：Android " + Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT
                + "\n系统构建：" + Build.DISPLAY
                + "\n省电模式：" + power.isPowerSaveMode()
                + "\n不受电池优化限制：" + power.isIgnoringBatteryOptimizations(context.getPackageName())
                + "\n精确位置权限：" + (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                + "\n\n当前状态：\n" + current + "\n\n上次保存的记录（可能早于当前状态）：\n" + previous(context);
        try {
            List<ApplicationExitInfo> exits = context.getSystemService(ActivityManager.class)
                    .getHistoricalProcessExitReasons(context.getPackageName(), 0, 1);
            if (!exits.isEmpty()) {
                ApplicationExitInfo exit = exits.get(0);
                result += "\n\n最近进程退出：" + new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(new Date(exit.getTimestamp()))
                        + " · reason=" + exit.getReason() + "（历史记录，不一定属于本次运行）";
            }
        } catch (RuntimeException ignored) { result += "\n进程退出记录不可用"; }
        return result;
    }

    static String stamp() {
        return new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
    }
}
