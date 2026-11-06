package app.containerlab;

import java.util.*;
import java.text.SimpleDateFormat;

final class ExitSummary {
    static final class Record {
        final long timestamp;
        final int pid, reason, status;
        final String process, description;
        Record(long timestamp, int pid, String process, int reason, int status, String description) {
            this.timestamp = timestamp; this.pid = pid; this.process = process;
            this.reason = reason; this.status = status; this.description = description;
        }
    }
    static List<Record> select(List<Record> records) {
        List<Record> sorted = new ArrayList<>();
        for (Record record : records) if (record.reason != 15 && record.reason != 16) sorted.add(record);
        Collections.sort(sorted, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        List<Record> selected = new ArrayList<>();
        for (Record record : sorted) {
            if (record.reason == 10 || record.reason == 11) continue;
            selected.add(record);
            if (selected.size() == 2) break;
        }
        if (selected.isEmpty() && !sorted.isEmpty()) selected.add(sorted.get(0));
        return selected;
    }

    static String format(List<Record> records, String nativeTrace) {
        if (records.isEmpty()) return "系统退出：暂无可读记录（不代表没有退出）";
        StringBuilder out = new StringBuilder("系统退出记录（历史，请核对时间）：\n");
        SimpleDateFormat time = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA);
        for (Record r : records) {
            String process = r.process == null ? "未知进程" : r.process.replace("app.containerlab:", "");
            String status = "status=" + r.status;
            if (r.reason == 2 || r.reason == 5) status += "(" + signal(r.status) + ")";
            String line = time.format(new Date(r.timestamp)) + " " + clean(process, 24) + " pid=" + r.pid
                    + " reason=" + r.reason + "/" + reason(r.reason) + " " + status;
            if (r.description != null && !r.description.isEmpty()) line += " " + clean(r.description, 38);
            out.append(clean(line, 115)).append('\n');
        }
        if (nativeTrace != null && !nativeTrace.isEmpty()) out.append("原生线索：").append(clean(nativeTrace, 240));
        return cleanMultiline(out.toString(), 500);
    }

    private static String reason(int reason) {
        switch (reason) {
            case 1: return "主动退出";
            case 2: return "信号终止";
            case 3: return "内存不足";
            case 4: return "Java崩溃";
            case 5: return "原生崩溃";
            case 6: return "ANR";
            case 7: return "初始化失败";
            case 8: return "权限变化";
            case 9: return "资源使用过量";
            case 10: return "用户请求终止";
            case 11: return "用户停止";
            case 12: return "依赖进程退出";
            case 13: return "其他系统终止";
            case 14: return "冻结器终止";
            case 15: return "应用变更";
            case 16: return "应用更新";
            default: return "未知";
        }
    }

    private static String signal(int status) {
        switch (status) {
            case 6: return "SIGABRT";
            case 9: return "SIGKILL";
            case 11: return "SIGSEGV";
            case 7: return "SIGBUS";
            case 4: return "SIGILL";
            case 5: return "SIGTRAP";
            default: return "信号待确认";
        }
    }

    private static String clean(String text, int max) {
        return cleanMultiline(text == null ? "" : text.replaceAll("\\s+", " ").trim(), max);
    }
    private static String cleanMultiline(String text, int max) {
        text = DiagnosticRedactor.redact(text);
        if (text.length() <= max) return text;
        int end = max - 1;
        if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end) + "…";
    }
}
