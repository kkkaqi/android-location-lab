package app.containerlab;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

final class RecentErrorSummary {
    private static final Pattern LINE = Pattern.compile("^(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\.\\d+\\s+(\\d+)\\s+\\d+\\s+[EF]\\s+([^:]+):\\s?(.*)$");
    private static final Pattern EXCEPTION = Pattern.compile("^(?:[\\w$]+\\.)*[\\w$]*(?:Exception|Error)(?::|\\s|$)");
    static String summarize(String log, int pid, String time) {
        SimpleDateFormat dates = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA);
        dates.setLenient(false);
        try {
            long exit = dates.parse(time).getTime();
            String cause = "", frame = "", last = "", seen = "";
            for (String line : DiagnosticRedactor.redact(log).split("\\r?\\n")) {
                Matcher m = LINE.matcher(line);
                if (!m.matches() || !m.group(2).equals(Integer.toString(pid))) continue;
                long delta = exit - dates.parse(m.group(1)).getTime();
                if (delta < -3000 || delta > 30000) continue;
                String message = m.group(4).trim();
                if (m.group(3).trim().equals("ContainerLab")) continue;
                last = m.group(3).trim() + ": " + message;
                if (message.startsWith("at ")) {
                    if (frame.isEmpty() && (message.contains("com.tencent.") || message.contains("com.tegufy.") || message.contains("org.chromium."))) frame = message;
                } else if (message.startsWith("Caused by:") || EXCEPTION.matcher(message).find()) {
                    cause = message; frame = ""; seen = m.group(1);
                }
            }
            if (last.isEmpty()) return "退出前错误：未读到匹配记录（不代表没有错误）";
            return clip("退出前错误 pid=" + pid + " " + (seen.isEmpty() ? time : seen) + "："
                    + clip(cause.isEmpty() ? last : cause, 150) + (frame.isEmpty() ? "" : " | " + clip(frame, 95)), 300);
        } catch (Exception error) { return "退出前错误：未读到可匹配时间的记录"; }
    }
    private static String clip(String text, int max) {
        text = DiagnosticRedactor.redact(text).replaceAll("\\s+", " ").trim();
        if (text.length() <= max) return text;
        int end = max - 1;
        if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end) + "…";
    }
}
