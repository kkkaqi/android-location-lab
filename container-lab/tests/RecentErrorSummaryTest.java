package app.containerlab;

public final class RecentErrorSummaryTest {
    private static int checks;
    private static void check(boolean pass, String message) { checks++; if (!pass) throw new AssertionError(message); }
    public static void main(String[] args) {
        String log = "11-03 18:05:53.001 29155 29155 E AndroidRuntime: FATAL EXCEPTION: main\n"
                + "11-03 18:05:53.002 29155 29155 E AndroidRuntime: java.lang.RuntimeException: wrapper\n"
                + "11-03 18:05:53.003 29155 29155 E AndroidRuntime: Caused by: java.lang.SecurityException: MISSING_PROVIDER ticket=DUMMY_SECRET\n"
                + "11-03 18:05:53.004 29155 29155 E AndroidRuntime: at com.tencent.mm.Fail.start(Fail.java:42)\n"
                + "11-03 18:05:54.001 99999 99999 E Other: OTHER_PROCESS_SECRET\n"
                + "11-03 14:00:00.001 29155 29155 E Old: OLD_SAME_PID\n";
        String summary = RecentErrorSummary.summarize(log, 29155, "11-03 18:05:55");
        check(summary.contains("SecurityException") && summary.contains("MISSING_PROVIDER"), "deepest cause retained");
        check(summary.contains("Fail.java:42"), "key application frame retained");
        check(!summary.contains("OTHER_PROCESS_SECRET") && !summary.contains("OLD_SAME_PID"), "only selected PID near exit time");
        check(!summary.contains("DUMMY_SECRET") && summary.length() <= 350, "redacted and bounded");
        check(RecentErrorSummary.summarize(log, 1, "11-03 18:05:55").contains("未读到"), "empty selection not absence of failure");
        check(RecentErrorSummary.summarize("logcat: Permission denied", 29155, "11-03 18:05:55").contains("未读到"), "logcat errors not attributed to WeChat");
        summary = RecentErrorSummary.summarize(log.replace("Fail.start(Fail.java:42)", "ErrorHandler.handle(ErrorHandler.java:42)"), 29155, "11-03 18:05:55");
        check(summary.contains("MISSING_PROVIDER") && summary.contains("ErrorHandler.java:42"), "stack class containing Error must not replace root cause");
        System.out.println("PASS: " + checks + " recent error selection checks");
    }
}
