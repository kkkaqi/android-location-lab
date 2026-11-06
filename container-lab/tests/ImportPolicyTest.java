package app.containerlab;

public final class ImportPolicyTest {
    private static int checks;
    private static void check(boolean ok, String reason) {
        checks++; if (!ok) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        check(ImportPolicy.importConfirmed(true, "com.tencent.mm", true), "installed WeChat with matching result succeeds");
        check(!ImportPolicy.importConfirmed(true, null, true), "upstream default success with null package is not success");
        check(!ImportPolicy.importConfirmed(true, "other.app", true), "wrong package is not success");
        check(!ImportPolicy.importConfirmed(true, "com.tencent.mm", false), "result must be backed by installed state");
        check(!ImportPolicy.importConfirmed(false, "com.tencent.mm", true), "failure remains failure even when an older guest exists");
        check(ImportPolicy.architectureError(new String[]{"arm64-v8a"}, new String[]{"arm64-v8a"}) == null, "ARM64 WeChat accepted");
        check(ImportPolicy.architectureError(new String[]{"arm64-v8a"}, new String[]{"armeabi-v7a"}) != null, "32-bit guest is rejected by ARM64 host");
        check(ImportPolicy.architectureError(new String[]{"x86_64"}, new String[]{"arm64-v8a"}) != null, "unsupported host rejected");
        check(ImportPolicy.architectureError(new String[]{"arm64-v8a"}, new String[]{}) != null, "unknown guest ABI requires diagnosis rather than a success claim");
        System.out.println("PASS: " + checks + " container policy checks");
    }
}
