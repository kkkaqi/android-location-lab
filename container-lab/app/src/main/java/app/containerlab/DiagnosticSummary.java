package app.containerlab;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Summarizes existing private records; does not infer crash causes or guest UI success. */
final class DiagnosticSummary {
    static final int MAX_CHARS = 1600;
    private static final Pattern EVENT = Pattern.compile("(?m)^(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) \\[([^\\]\\r\\n]+)\\] ?");
    private static final Pattern NETWORK = Pattern.compile("INTERNET=(-?\\d+)，ACCESS_NETWORK_STATE=(-?\\d+)");
    private static final Pattern APP_CLASS = Pattern.compile("class=([^。\\s]+)");

    static String summarize(String header, String raw) {
        return summarize(header, raw, "");
    }

    static String summarize(String header, String raw, String systemEvidence) {
        List<Entry> entries = parse(DiagnosticRedactor.redact(raw));
        StringBuilder out = new StringBuilder();
        add(out, DiagnosticRedactor.redact(header).trim(), 220);
        if (systemEvidence != null && !systemEvidence.isEmpty()) add(out, DiagnosticRedactor.redact(systemEvidence), 500);
        if (entries.isEmpty()) {
            add(out, "尚无运行记录", 40);
            return out.toString();
        }
        int start = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if ("app.containerlab".equals(e.process) && (e.body.startsWith("引擎 attach")
                    || e.body.startsWith("进程启动：") || e.body.startsWith("attach："))) start = i;
        }

        Set<String> errors = new HashSet<>();
        int errorCount = 0;
        for (int i = entries.size() - 1; i >= 0 && errorCount < 2; i--) {
            Entry e = entries.get(i);
            if (!isError(e.body)) continue;
            String detail = errorDetail(e.body);
            if (!errors.add(detail)) continue;
            add(out, (i < start ? "历史错误 " : "最近错误 ") + e.time + " " + clip(processName(e.process), 30) + "：" + detail,
                    systemEvidence == null || systemEvidence.isEmpty() ? 260 : 180);
            errorCount++;
        }
        if (errorCount == 0) add(out, "错误：未记录到异常；不能排除原生崩溃或主动退出。", 70);

        String version = "", appClass = "", other = "", locationResult = "";
        boolean imported = false, hostCreated = false, serverCreated = false;
        int launches = 0;
        Map<String, Integer> processes = new LinkedHashMap<>();
        Map<String, String> permissions = new LinkedHashMap<>();
        for (int i = start; i < entries.size(); i++) {
            Entry e = entries.get(i);
            String body = e.body;
            boolean known = true;
            if (body.startsWith("本机微信版本=")) version = body.replace("本机微信版本=", "微信：").replace("，APK数量=", " · APK=").replace("，native ABI=", " · ");
            else if (body.startsWith("容器安装记录已确认")) imported = true;
            else if (body.startsWith("容器启动请求已返回")) { launches++; locationResult = ""; }
            else if (body.startsWith("容器定位：")) { locationResult = ""; }
            else if (body.startsWith("访客定位结果：")) locationResult = "定位核验 " + e.time + "：" + body.substring("访客定位结果：".length());
            else if (body.startsWith("访客定位核验") || body.startsWith("定位适配核验")) { /* raw detail stays in the local log */ }
            else if (body.startsWith("访客 Application 回调已返回")) {
                if ("com.tencent.mm".equals(e.process)) locationResult = "";
                processes.put(e.process, processes.containsKey(e.process) ? processes.get(e.process) + 1 : 1);
                Matcher matcher = APP_CLASS.matcher(body);
                if ("com.tencent.mm".equals(e.process) && matcher.find()) appClass = matcher.group(1);
            } else if (body.startsWith("访客 Context 联网权限")) {
                Matcher matcher = NETWORK.matcher(body);
                permissions.put(e.process, matcher.find() ? matcher.group(1) + "/" + matcher.group(2) : "结果未知");
            } else if (body.startsWith("访客联网权限检查：")) {
                permissions.put(e.process, "检查异常");
            } else if (body.startsWith("引擎 create")) {
                if ("app.containerlab".equals(e.process)) hostCreated = true;
                if ("app.containerlab:black".equals(e.process)) serverCreated = true;
            } else if (!body.startsWith("引擎 attach") && !body.startsWith("进程启动：")) known = false;
            if (!known && !isError(body)) other = e.time + " " + processName(e.process) + " " + singleLine(body);
        }
        if (version.isEmpty()) {
            for (int i = start - 1; i >= 0; i--) {
                String body = entries.get(i).body;
                if (body.startsWith("本机微信版本=")) {
                    version = body.replace("本机微信版本=", "微信（上次导入）：").replace("，APK数量=", " · APK=").replace("，native ABI=", " · ");
                    break;
                }
            }
        }
        if (!locationResult.isEmpty()) add(out, locationResult, 150);
        if (!version.isEmpty()) add(out, version, 120);
        add(out, "本次起点：" + entries.get(start).time + "；最近记录：" + entries.get(entries.size() - 1).time, 80);
        add(out, "导入确认：" + (imported ? "已记录" : "本次未记录") + "；启动请求×" + launches + "（页面未验证）", 85);
        add(out, permissionSummary(permissions), 190);
        StringBuilder processText = new StringBuilder("进程：");
        for (Map.Entry<String, Integer> p : processes.entrySet()) {
            if (processText.length() > 3) processText.append("；");
            processText.append(processName(p.getKey())).append('×').append(p.getValue());
        }
        if (processes.isEmpty()) processText.append("本次未记录到访客回调");
        add(out, processText.toString(), 180);
        if (!appClass.isEmpty()) add(out, "主进程 Application：" + appClass, 100);
        add(out, "引擎 create：宿主" + (hostCreated ? "已返回" : "未记录") + "，管理服务" + (serverCreated ? "已返回" : "未记录"), 75);
        if (!other.isEmpty()) add(out, "其他最近记录：" + other, 130);
        add(out, "仅摘要；重复记录已合并，完整记录保留在手机本地。", 60);
        return out.toString();
    }

    private static String permissionSummary(Map<String, String> permissions) {
        if (permissions.isEmpty()) return "联网权限：本次未记录";
        StringBuilder failed = new StringBuilder();
        int allowed = 0;
        for (Map.Entry<String, String> p : permissions.entrySet()) {
            if ("0/0".equals(p.getValue())) allowed++;
            else {
                if (failed.length() > 0) failed.append('；');
                failed.append(processName(p.getKey())).append('=').append(p.getValue());
            }
        }
        return failed.length() == 0
                ? "联网权限：" + allowed + "个进程最近检查均为0/0（允许）"
                : "联网权限：" + failed + "；其余" + allowed + "个进程0/0";
    }

    private static String errorDetail(String body) {
        String[] lines = body.split("\\r?\\n");
        String cause = lines[0], frame = "";
        for (String line : lines) {
            String clean = line.trim();
            if (clean.startsWith("Caused by:")) { cause = clean; frame = ""; }
            if (frame.isEmpty() && clean.startsWith("at ")
                    && (clean.contains("com.tencent.mm.") || clean.contains("com.tegufy.blackbox.") || clean.contains("app.containerlab."))) frame = clean;
        }
        return clip(singleLine(cause), 110) + (frame.isEmpty() ? "" : " | " + clip(frame, 95))
                + (cause.equals(lines[0]) ? "" : " | 外层：" + clip(singleLine(lines[0]), 45));
    }

    private static boolean isError(String body) {
        return body.contains("Exception") || body.contains("Error") || body.contains("异常") || body.contains("失败")
                || body.contains("FATAL") || body.contains("Fatal") || body.contains("SIGSEGV") || body.contains("SIGABRT")
                || body.contains("net::ERR_");
    }

    private static String processName(String name) {
        if ("com.tencent.mm".equals(name)) return "微信主进程";
        if (name.startsWith("com.tencent.mm:")) return name.substring("com.tencent.mm:".length())
                .replace("xweb_privileged_process_", "XWeb特权").replace("xweb_sandboxed_process_", "XWeb沙箱");
        if ("app.containerlab".equals(name)) return "宿主";
        return name.replace("app.containerlab:", "容器:");
    }

    private static List<Entry> parse(String raw) {
        List<Entry> result = new ArrayList<>();
        Matcher matcher = EVENT.matcher(raw);
        Entry previous = null;
        int bodyStart = 0;
        while (matcher.find()) {
            String body = raw.substring(bodyStart, matcher.start()).trim();
            if (previous != null) previous.body = body;
            else if (!body.isEmpty()) result.add(new Entry("时间未知", "未知", body));
            previous = new Entry(matcher.group(1), matcher.group(2), "");
            result.add(previous);
            bodyStart = matcher.end();
        }
        String tail = raw.substring(bodyStart).trim();
        if (previous != null) previous.body = tail;
        else if (!tail.isEmpty()) result.add(new Entry("时间未知", "未知", tail));
        return result;
    }

    private static String singleLine(String text) { return text.replaceAll("\\s+", " ").trim(); }

    private static void add(StringBuilder out, String line, int limit) {
        int remaining = MAX_CHARS - out.length() - 1;
        if (remaining > 0) out.append(clip(line, Math.min(limit, remaining))).append('\n');
    }

    private static String clip(String text, int limit) {
        if (text.length() <= limit) return text;
        if (limit <= 1) return "…";
        int end = limit - 1;
        if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end) + "…";
    }

    private static final class Entry {
        final String time, process;
        String body;
        Entry(String time, String process, String body) {
            this.time = time; this.process = process; this.body = body;
        }
    }
}
