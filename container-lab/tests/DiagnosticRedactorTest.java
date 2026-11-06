package app.containerlab;

public final class DiagnosticRedactorTest {
    public static void main(String[] args) {
        String input = "https://example.invalid/check?ticket=DUMMY_SECRET&lang=zh_CN&code=DUMMY_CODE";
        String clean = DiagnosticRedactor.redact(input);
        if (clean.contains("DUMMY_SECRET") || clean.contains("DUMMY_CODE")) throw new AssertionError("login parameters must be redacted");
        if (!clean.contains("lang=zh_CN")) throw new AssertionError("non-sensitive context remains");
        if (DiagnosticRedactor.redact("phone=13800000000 access_token=DUMMY_ACCESS").contains("13800000000")) throw new AssertionError("phone redacted");
        if (DiagnosticRedactor.redact("phone=13800000000 access_token=DUMMY_ACCESS").contains("DUMMY_ACCESS")) throw new AssertionError("access token redacted");
        if (!"permission=0 uid=10123".equals(DiagnosticRedactor.redact("permission=0 uid=10123"))) throw new AssertionError("safe diagnostic unchanged");
        if (DiagnosticRedactor.redact("https://example.invalid/?token=DUMMY_BARE_TOKEN").contains("DUMMY_BARE_TOKEN")) throw new AssertionError("bare token parameter redacted");
        System.out.println("PASS: 6 diagnostic redaction checks");
    }
}
