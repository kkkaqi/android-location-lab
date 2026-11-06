package app.containerlab;

import android.app.ActivityManager;
import android.app.Application;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import java.io.*;
import java.util.*;

/** Reads only host-owned system exit history, on the host activity's background worker. */
final class SystemExitDiagnostics {
    static String collect(Context context) {
        if (Build.VERSION.SDK_INT < 30) return "系统退出：Android版本不支持读取";
        if (!"app.containerlab".equals(Application.getProcessName())) return "系统退出：仅在宿主主界面读取";
        try {
            ActivityManager manager = context.getSystemService(ActivityManager.class);
            if (manager == null) return "系统退出：系统服务不可用";
            List<ApplicationExitInfo> history = manager.getHistoricalProcessExitReasons("app.containerlab", 0, 32);
            List<ExitSummary.Record> records = new ArrayList<>();
            Map<ExitSummary.Record, ApplicationExitInfo> originals = new IdentityHashMap<>();
            for (ApplicationExitInfo info : history) {
                if (info.getPackageUid() != android.os.Process.myUid()) continue;
                ExitSummary.Record record = new ExitSummary.Record(info.getTimestamp(), info.getPid(), info.getProcessName(),
                        info.getReason(), info.getStatus(), info.getDescription());
                records.add(record); originals.put(record, info);
            }
            List<ExitSummary.Record> selected = ExitSummary.select(records);
            String nativeTrace = "";
            if (Build.VERSION.SDK_INT >= 31) {
                for (ExitSummary.Record record : selected) {
                    if (record.reason != ApplicationExitInfo.REASON_CRASH_NATIVE) continue;
                    nativeTrace = "pid=" + record.pid + " " + trace(originals.get(record));
                    break;
                }
            }
            String exits = ExitSummary.format(selected, nativeTrace);
            if (!selected.isEmpty() && (selected.get(0).reason == ApplicationExitInfo.REASON_EXIT_SELF
                    || selected.get(0).reason == ApplicationExitInfo.REASON_CRASH)) {
                ExitSummary.Record record = selected.get(0);
                return ExitSummary.format(Collections.singletonList(record), "")
                        + "\n" + CrashEvidence.recentErrors(record.pid, record.timestamp);
            }
            return exits;
        } catch (Exception error) {
            return "系统退出：读取失败（" + error.getClass().getSimpleName() + "），不是没有退出记录";
        }
    }

    private static String trace(ApplicationExitInfo info) {
        try (InputStream input = info.getTraceInputStream()) {
            if (input == null) return "系统未保留可读原生堆栈";
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count > NativeCrashSummary.MAX_BYTES - output.size()) return "原生记录过大，未解析";
                output.write(buffer, 0, count);
            }
            return NativeCrashSummary.summarize(output.toByteArray());
        } catch (Exception error) {
            return "原生堆栈读取失败（" + error.getClass().getSimpleName() + "）";
        }
    }
}
