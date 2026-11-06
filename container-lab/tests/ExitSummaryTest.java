package app.containerlab;

import java.util.*;

public final class ExitSummaryTest {
    private static int checks;
    private static void check(boolean pass, String message) {
        checks++;
        if (!pass) throw new AssertionError(message);
    }
    private static ExitSummary.Record record(long time, int pid, int reason, int status, String description) {
        return new ExitSummary.Record(time, pid, "app.containerlab:p0", reason, status, description);
    }
    public static void main(String[] args) {
        ExitSummary.Record nativeCrash = record(1000, 11, 5, 11, "native failure");
        ExitSummary.Record selfExit = record(2000, 12, 1, 7, "exit requested");
        ExitSummary.Record upgrade = record(3000, 13, 16, 0, "package updated");
        List<ExitSummary.Record> selected = ExitSummary.select(Arrays.asList(nativeCrash, upgrade, selfExit));
        check(selected.size() == 2 && selected.get(0) == selfExit && selected.get(1) == nativeCrash, "newer upgrade records cannot hide older crash evidence; newest selected first");
        String text = ExitSummary.format(selected, "SIGSEGV libexample.so+0x123");
        check(text.contains("reason=5") && text.contains("原生崩溃") && text.contains("SIGSEGV"), "native reason and signal preserved");
        check(text.contains("reason=1") && text.contains("status=7") && text.contains("主动退出"), "exit status remains distinct from reason and signal");
        check(text.contains("pid=11") && text.contains("历史"), "historical process identity explicit");
        check(text.length() <= 500, "system exit evidence has a bounded budget");
        check(ExitSummary.format(Collections.<ExitSummary.Record>emptyList(), "").contains("不代表没有退出"), "empty history is not proof of no exit");
        check(ExitSummary.select(Arrays.asList(upgrade)).isEmpty(), "installation termination is not presented as crash evidence");
        StringBuilder sensitive = new StringBuilder("ticket=DUMMY_SECRET&code=DUMMY_CODE ");
        for (int i = 0; i < 1500; i++) sensitive.append('x');
        text = ExitSummary.format(Arrays.asList(record(2000, 99, 2, 9, sensitive.toString())), "");
        check(text.length() <= 500 && !text.contains("DUMMY_SECRET") && !text.contains("DUMMY_CODE"), "redaction before truncation and bounded descriptions");
        check(text.contains("SIGKILL") && !text.contains("SIGSEGV"), "kill signal not mislabeled as segmentation fault");
        check(ExitSummary.format(Arrays.asList(record(2000, 99, 0, 0, "")), "").contains("未知"), "unknown reason has no invented explanation");
        System.out.println("PASS: " + checks + " system exit summary checks");
    }
}
