package app.containerlab;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.*;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

final class LabLog {
    private static final int LIMIT = 160000;
    static void append(Context context, String message) {
        message = DiagnosticRedactor.redact(message);
        Log.i("ContainerLab", message);
        try {
            File file = new File(context.getFilesDir(), "container-lab.log");
            try (RandomAccessFile output = new RandomAccessFile(file, "rw"); FileLock lock = output.getChannel().lock()) {
                if (output.length() > LIMIT) output.setLength(0);
                output.seek(output.length());
                String time = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
                output.write((time + " [" + android.app.Application.getProcessName() + "] " + message + "\n").getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception error) { Log.w("ContainerLab", "Cannot save local diagnostic", error); }
    }
    static void error(Context context, String stage, Throwable error) {
        append(context, stage + "：" + Log.getStackTraceString(error));
    }
    static String report(Context context) {
        return report(context, "");
    }
    static String report(Context context, String systemEvidence) {
        String header = "容器诊断 0.2.4 · " + new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(new Date())
                + "\n" + Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT
                + "\n构建：" + Build.DISPLAY + "\n" + Arrays.toString(Build.SUPPORTED_ABIS)
                + " · 宿主64位=" + android.os.Process.is64Bit() + "\n" + ContainerLocationSettings.summary(context) + "\n";
        try {
            File file = new File(context.getFilesDir(), "container-lab.log");
            return DiagnosticSummary.summarize(header, file.exists() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : "", systemEvidence);
        } catch (IOException error) { return DiagnosticSummary.summarize(header, "无法读取诊断：" + error, systemEvidence); }
    }
}
