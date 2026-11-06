package app.containerlab;

public final class DiagnosticSummaryTest {
    private static int checks;
    private static String event(String time, String process, String message) {
        return "11-03 " + time + " [" + process + "] " + message + "\n";
    }
    private static void check(boolean pass, String message) {
        checks++;
        if (!pass) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        String header = "容器诊断 0.1.2\nOnePlus PKG110 / Android 15\n";
        String old = event("11:00:00", "app.containerlab", "引擎 attach 调用已返回（不代表访客应用已启动）")
                + event("11:00:01", "app.containerlab", "本机微信版本=OLD_VERSION，APK数量=1，native ABI=[arm64-v8a]")
                + event("11:01:00", "com.tencent.mm:old_process", "访客 Application 回调已返回：process=com.tencent.mm:old_process，class=com.tencent.mm.app.Application。不代表微信真实初始化或页面可用。");
        String run = event("13:36:00", "app.containerlab", "引擎 attach 调用已返回（不代表访客应用已启动）")
                + event("13:36:01", "app.containerlab", "引擎 create 调用已返回")
                + event("13:36:50", "app.containerlab", "本机微信版本=8.0.78，APK数量=1，native ABI=[arm64-v8a]")
                + event("13:36:52", "app.containerlab", "容器安装记录已确认。下一步点击请求启动，并观察实际页面。")
                + event("13:36:55", "app.containerlab", "容器启动请求已返回，请观察实际页面；这不是启动成功的确认。");
        String callback = event("13:37:13", "com.tencent.mm:xweb_privileged_process_0", "访客 Application 回调已返回：process=com.tencent.mm:xweb_privileged_process_0，class=com.tencent.mm.app.Application。不代表微信真实初始化或页面可用。")
                + event("13:37:13", "com.tencent.mm:xweb_privileged_process_0", "访客 Context 联网权限：INTERNET=0，ACCESS_NETWORK_STATE=0（0=系统确认授予）；不代表安全验证已完成。");
        StringBuilder raw = new StringBuilder(old).append(run);
        for (int i = 0; i < 300; i++) raw.append(callback);
        String summary = DiagnosticSummary.summarize(header, raw.toString());
        check(summary.length() <= 1600, "mobile report must fit 1600 characters");
        check(summary.contains("8.0.78"), "WeChat version retained");
        check(!summary.contains("OLD_VERSION") && !summary.contains("old_process"), "old successful run omitted");
        check(summary.contains("×300"), "repeated process starts counted");
        check(summary.contains("均为0/0"), "successful network checks summarized");
        check(summary.contains("页面未验证"), "launch return must not imply UI success");
        check(summary.contains("不能排除"), "missing exceptions do not prove no crashes");

        String error = event("13:38:00", "com.tencent.mm", "进程未捕获异常：java.lang.RuntimeException: wrapper\n"
                + "\tat android.app.ActivityThread.main(ActivityThread.java:1)\n"
                + "Caused by: java.lang.IllegalStateException: inner\n"
                + "Caused by: java.lang.UnsatisfiedLinkError: missing libexample.so\n"
                + "\tat com.tencent.mm.Example.run(Example.java:42)");
        String noise = event("13:39:00", "app.containerlab", "引擎 create 调用已返回");
        summary = DiagnosticSummary.summarize(header, raw + error + noise);
        check(summary.contains("UnsatisfiedLinkError") && summary.contains("missing libexample.so"), "deepest cause retained ahead of routine events");
        check(summary.contains("Example.java:42"), "useful guest stack frame retained");
        check(summary.indexOf("missing libexample.so") < summary.indexOf("\n进程："), "errors precede routine process summary");
        summary = DiagnosticSummary.summarize(header, error + run);
        check(summary.contains("历史") && summary.contains("missing libexample.so"), "previous crash survives a host restart with historical label");

        String denied = event("13:40:00", "com.tencent.mm:push", "访客 Context 联网权限：INTERNET=-1，ACCESS_NETWORK_STATE=0（0=系统确认授予）；不代表安全验证已完成。");
        summary = DiagnosticSummary.summarize(header, run + callback + denied);
        check(summary.contains("push=-1/0") && !summary.contains("均为0/0"), "denial must not be hidden by successful checks");
        summary = DiagnosticSummary.summarize(header, run + event("13:41:00", "com.tencent.mm", "网页错误 net::ERR_CACHE_MISS https://example.invalid/?ticket=DUMMY_SECRET&code=DUMMY_CODE"));
        check(summary.contains("ERR_CACHE_MISS") && !summary.contains("DUMMY_SECRET") && !summary.contains("DUMMY_CODE"), "redact sensitive parameters while retaining error code");

        StringBuilder hugeError = new StringBuilder("java.lang.RuntimeException: ");
        for (int i = 0; i < 10000; i++) hugeError.append('x');
        hugeError.append("\nCaused by: java.lang.IllegalStateException: DEEPEST_CAUSE\n\tat com.tencent.mm.Foo.run(Foo.java:9)");
        summary = DiagnosticSummary.summarize(header, run + event("13:42:00", "com.tencent.mm", hugeError.toString()));
        check(summary.length() <= 1600 && summary.contains("DEEPEST_CAUSE"), "large exception shortened without losing root cause");
        check(DiagnosticSummary.summarize(header, "").contains("尚无运行记录"), "empty log has explicit state");
        summary = DiagnosticSummary.summarize(header, "legacy plain text\nlast partial line");
        check(summary.contains("last partial line"), "unrecognized records remain visible");
        summary = DiagnosticSummary.summarize(header, run + error + error + denied);
        check(summary.indexOf("missing libexample.so") == summary.lastIndexOf("missing libexample.so"), "duplicate errors collapsed");
        String newRun = event("14:00:00", "app.containerlab", "引擎 attach 调用已返回（不代表访客应用已启动）");
        summary = DiagnosticSummary.summarize(header, run + newRun);
        check(summary.contains("8.0.78") && summary.contains("上次导入"), "last known WeChat version survives host restart with an explicit label");
        String recovered = event("13:41:00", "com.tencent.mm:push", "访客 Context 联网权限：INTERNET=0，ACCESS_NETWORK_STATE=0（0=系统确认授予）；不代表安全验证已完成。");
        summary = DiagnosticSummary.summarize(header, run + denied + recovered);
        check(summary.contains("均为0/0") && !summary.contains("push=-1/0"), "network summary uses latest check per process");
        StringBuilder longName = new StringBuilder("com.tencent.mm:");
        for (int i = 0; i < 400; i++) longName.append('x');
        summary = DiagnosticSummary.summarize(header, run + event("14:01:00", longName.toString(), "java.lang.IllegalStateException: IMPORTANT_FAILURE"));
        check(summary.contains("IMPORTANT_FAILURE"), "long process name cannot displace failure message");
        String failedAttach = event("14:02:00", "app.containerlab", "attach：java.lang.IllegalStateException: INIT_FAILED");
        summary = DiagnosticSummary.summarize(header, run + callback + failedAttach);
        check(summary.contains("导入确认：本次未记录") && summary.contains("启动请求×0") && !summary.contains("均为0/0"), "failed host attach starts a new run without borrowing old successes");
        summary = DiagnosticSummary.summarize(header, run + callback
                + event("14:03:00", "com.tencent.mm:xweb_privileged_process_0", "访客联网权限检查：java.lang.SecurityException: CHECK_FAILED"));
        check(summary.contains("XWeb特权0=检查异常") && !summary.contains("均为0/0"), "latest permission exception overrides prior success");
        String repeatedError = "进程未捕获异常：java.lang.IllegalStateException: REPEATED_CAUSE";
        summary = DiagnosticSummary.summarize(header, run
                + event("14:04:00", "com.tencent.mm", "launch：java.lang.IllegalArgumentException: SECOND_KIND")
                + event("14:05:00", "com.tencent.mm:xweb_privileged_process_0", repeatedError)
                + event("14:06:00", "com.tencent.mm:xweb_privileged_process_1", repeatedError));
        check(summary.contains("SECOND_KIND"), "same failure in different processes must not evict a distinct failure");
        summary = DiagnosticSummary.summarize(header, run + event("14:07:00", "com.tencent.mm", "java.lang.RuntimeException: wrapper\n"
                + "\tat com.tegufy.blackbox.app.BActivityThread.bind(BActivityThread.java:1)\n"
                + "Caused by: java.lang.IllegalStateException: ROOT_CAUSE\n"
                + "\tat com.tencent.mm.Root.fail(Root.java:77)"));
        check(summary.contains("Root.java:77"), "key frame must belong to deepest cause rather than outer wrapper");
        String exits = "系统退出记录（历史，请核对时间）：\n11-03 14:02:29 p0 pid=42 reason=5/原生崩溃 status=11(SIGSEGV)\n原生线索：pid=42 libblackbox.so+0x123";
        summary = DiagnosticSummary.summarize(header, raw + error, exits);
        check(summary.contains("reason=5") && summary.contains("status=11") && summary.contains("libblackbox.so"), "system exit evidence included alongside Java log");
        check(summary.indexOf("reason=5") < summary.indexOf("\n进程："), "exit evidence precedes routine process records");
        check(summary.length() <= 1600, "system evidence shares original mobile budget");
        summary = DiagnosticSummary.summarize(header, "", exits + "\nticket=DUMMY_SYSTEM_SECRET");
        check(summary.contains("reason=5") && !summary.contains("DUMMY_SYSTEM_SECRET"), "system evidence survives empty private log and is redacted");
        String located = event("14:08:00", "com.tencent.mm", "访客定位结果：框架检查通过；页面未验证");
        summary = DiagnosticSummary.summarize(header, run + located + callback);
        check(summary.contains("定位核验 11-03 14:08:00：框架检查通过；页面未验证"), "location result survives later routine process logs");
        summary = DiagnosticSummary.summarize(header, run + located
                + event("14:09:00", "app.containerlab", "容器定位：已关闭") + callback);
        check(!summary.contains("框架检查通过"), "changed location configuration must invalidate old audit result");
        summary = DiagnosticSummary.summarize(header, run + located
                + event("14:09:00", "app.containerlab", "容器启动请求已返回，请观察实际页面") + callback);
        check(!summary.contains("框架检查通过"), "relaunch cannot borrow prior audit result");
        System.out.println("PASS: " + checks + " compact diagnostic checks");
    }
}
