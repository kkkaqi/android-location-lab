package app.fixedlocation;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.LocationManager;
import android.os.*;
import java.util.Arrays;
import java.util.Locale;

public final class MockLocationService extends Service {
    public static final String STOP = "app.fixedlocation.STOP";
    private static final String CHANNEL = "mock_location";
    private static final int NOTIFICATION = 10;

    public static final class State {
        public final boolean running;
        public final String message;
        public final long lastUpdate;
        State(boolean running, String message, long lastUpdate) {
            this.running = running; this.message = message; this.lastUpdate = lastUpdate;
        }
    }
    public static volatile State state = new State(false, "尚未开始。设置坐标后点击开始。", 0);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MockSession session;
    private boolean failed;
    private String description;
    private Coordinates coordinates;
    private final UpdateHealth health = new UpdateHealth();
    private String lastDiagnostic = "尚未完成首轮更新";
    private final Runnable update = new Runnable() {
        public void run() {
            try {
                LocationManager manager = getSystemService(LocationManager.class);
                if (!manager.isLocationEnabled()) throw new IllegalStateException("系统定位已关闭");
                session.tick();
                publish(manager);
                handler.postDelayed(this, 1000);
            } catch (RuntimeException e) { fail(e); }
        }
    };

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (session != null) return START_NOT_STICKY;
        failed = false;
        try {
            coordinates = Coordinates.parse(intent.getStringExtra("latitude"), intent.getStringExtra("longitude"));
            LocationManager manager = getSystemService(LocationManager.class);
            if (!manager.isLocationEnabled()) throw new IllegalStateException("请先开启手机的系统定位");
            NotificationManager notifications = getSystemService(NotificationManager.class);
            notifications.createNotificationChannel(new NotificationChannel(CHANNEL, "模拟定位运行状态", NotificationManager.IMPORTANCE_LOW));
            startForeground(NOTIFICATION, notification(coordinates, false), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            session = new MockSession(new AndroidBackend(this, manager), Arrays.asList("gps", "network", "fused"));
            session.start(coordinates);
            notifications.notify(NOTIFICATION, notification(coordinates, true));
            description = String.format(Locale.CHINA, "目标：%.6f, %.6f\n已注入来源：%s", coordinates.latitude, coordinates.longitude, String.join(" / ", session.providers()));
            if (!session.warnings().isEmpty()) description += "\n部分来源不可用：" + String.join("；", session.warnings());
            publish(manager);
            handler.postDelayed(update, 1000);
        } catch (RuntimeException e) { fail(e); }
        return START_NOT_STICKY;
    }

    private void publish(LocationManager manager) {
        long now = SystemClock.elapsedRealtime();
        health.record(now);
        lastDiagnostic = description + String.format(Locale.CHINA,
                "\n更新轮数：%d · 最近调用：%s\n最长更新间隔：%.1f秒（预期约1秒）\n",
                health.count(), RunDiagnostics.stamp(), health.maxGapMillis() / 1000d)
                + RunDiagnostics.readbacks(this, manager, session.providers(), coordinates);
        state = new State(true, lastDiagnostic, now);
        if (health.count() % 5 == 1) RunDiagnostics.save(this, "服务运行中\n" + lastDiagnostic);
    }

    private Notification notification(Coordinates coordinates, boolean running) {
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 2, new Intent(this, MockLocationService.class).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_location)
                .setContentTitle(running ? "定点 · 正在提供模拟位置" : "定点 · 正在启动模拟定位")
                .setContentText(String.format(Locale.CHINA, "%.6f, %.6f · 点此查看", coordinates.latitude, coordinates.longitude))
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(R.drawable.ic_location, "停止模拟定位", stop).build())
                .build();
    }

    private void fail(RuntimeException error) {
        failed = true;
        String message = error instanceof SecurityException
                ? "定位或模拟定位授权不足。请检查权限，并在开发者选项中选择「定点」。"
                : error.getMessage();
        if (message == null) message = error.getClass().getSimpleName();
        state = new State(false, "启动或更新失败：" + message, 0);
        RunDiagnostics.save(this, state.message + "\n失败前记录：\n" + lastDiagnostic);
        stopSelf();
    }

    @Override public void onTaskRemoved(Intent rootIntent) { stopSelf(); }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        String cleanup = "";
        if (session != null) {
            session.stop();
            if (!session.warnings().isEmpty()) cleanup = "\n" + String.join("；", session.warnings());
        }
        state = new State(false, (failed ? state.message : "已停止模拟定位，等待系统重新获取实际位置。") + cleanup, 0);
        RunDiagnostics.save(this, state.message + "\n停止前记录：\n" + lastDiagnostic);
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
