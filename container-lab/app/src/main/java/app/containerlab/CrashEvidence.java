package app.containerlab;

import android.app.Application;
import android.content.Context;
import java.io.*;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

final class CrashEvidence {
    static void install(Context context) {
        if (!Application.getProcessName().matches("app\\.containerlab:p\\d+")) return;
        try {
            Method get = Thread.class.getDeclaredMethod("getUncaughtExceptionPreHandler");
            Method set = Thread.class.getDeclaredMethod("setUncaughtExceptionPreHandler", Thread.UncaughtExceptionHandler.class);
            get.setAccessible(true); set.setAccessible(true);
            Thread.UncaughtExceptionHandler original = (Thread.UncaughtExceptionHandler) get.invoke(null);
            if (original instanceof CrashObserver) return;
            CrashObserver observer = new CrashObserver((thread, error) -> LabLog.error(context, "异常预处理 thread=" + thread.getName(), error), original);
            set.invoke(null, observer);
            LabLog.append(context, get.invoke(null) == observer ? "预处理观察：已接入" : "预处理观察：未确认接入");
        } catch (Exception error) {
            LabLog.append(context, "预处理观察：不可用（" + error.getClass().getSimpleName() + "）");
        }
    }

    static String recentErrors(int pid, long timestamp) {
        java.lang.Process child = null;
        try {
            child = new ProcessBuilder("/system/bin/logcat", "-d", "-b", "main", "-b", "crash", "-t", "400",
                    "--uid=" + android.os.Process.myUid(), "-v", "threadtime", "*:E").redirectErrorStream(true).start();
            final InputStream input = child.getInputStream();
            final StringBuilder tail = new StringBuilder();
            Thread reader = new Thread(() -> {
                try (Reader stream = new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)) {
                    char[] buffer = new char[2048];
                    int count;
                    while ((count = stream.read(buffer)) != -1) {
                        synchronized (tail) {
                            tail.append(buffer, 0, count);
                            if (tail.length() > 64000) tail.delete(0, tail.length() - 64000);
                        }
                    }
                } catch (IOException ignored) { }
            }, "container-error-reader");
            reader.setDaemon(true); reader.start();
            if (!child.waitFor(2500, TimeUnit.MILLISECONDS)) {
                child.destroyForcibly();
                return "退出前错误：读取超时";
            }
            reader.join(300);
            if (reader.isAlive()) return "退出前错误：日志读取未完成";
            if (child.exitValue() != 0) return "退出前错误：logcat不可读（退出码" + child.exitValue() + "）";
            String time = new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.CHINA).format(new java.util.Date(timestamp));
            synchronized (tail) { return RecentErrorSummary.summarize(tail.toString(), pid, time); }
        } catch (Exception error) {
            return "退出前错误：读取不可用（" + error.getClass().getSimpleName() + "）";
        } finally {
            if (child != null) child.destroy();
        }
    }
}
