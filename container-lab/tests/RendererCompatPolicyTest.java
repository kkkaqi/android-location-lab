package app.containerlab;

public final class RendererCompatPolicyTest {
    private static int checks;
    private static void expect(boolean expected, boolean actual) {
        checks++;
        if (expected != actual) throw new AssertionError("scope check " + checks);
    }
    public static void main(String[] args) {
        expect(true, RendererCompatPolicy.applies(true, 0, "com.tencent.mm", "com.tencent.mm:xweb_sandboxed_process_0", 35, "OnePlus"));
        expect(true, RendererCompatPolicy.applies(true, 0, "com.tencent.mm", "com.tencent.mm:xweb_sandboxed_process_12", 35, "OPPO"));
        expect(false, RendererCompatPolicy.applies(false, 0, "com.tencent.mm", "com.tencent.mm:xweb_sandboxed_process_0", 35, "OnePlus"));
        expect(false, RendererCompatPolicy.applies(true, 1, "com.tencent.mm", "com.tencent.mm:xweb_sandboxed_process_0", 35, "OnePlus"));
        expect(false, RendererCompatPolicy.applies(true, 0, "app.other", "com.tencent.mm:xweb_sandboxed_process_0", 35, "OnePlus"));
        for (String process : new String[]{null, "com.tencent.mm", "com.tencent.mm:push", "com.tencent.mm:xweb_privileged_process_0", "com.tencent.mm:xweb_sandboxed_process_", "com.tencent.mm:xweb_sandboxed_process_0:other"})
            expect(false, RendererCompatPolicy.applies(true, 0, "com.tencent.mm", process, 35, "OnePlus"));
        expect(false, RendererCompatPolicy.applies(true, 0, "com.tencent.mm", "com.tencent.mm:xweb_sandboxed_process_0", 34, "OnePlus"));
        expect(false, RendererCompatPolicy.applies(true, 0, "com.tencent.mm", "com.tencent.mm:xweb_sandboxed_process_0", 35, "Google"));
        expect(false, RendererCompatPolicy.applies(true, 0, "com.tencent.mm", "com.tencent.mm:xweb_sandboxed_process_0", 35, null));
        System.out.println("PASS: " + checks + " renderer compatibility scope checks");
    }
}
