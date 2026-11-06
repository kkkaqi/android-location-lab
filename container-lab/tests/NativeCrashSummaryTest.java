package app.containerlab;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class NativeCrashSummaryTest {
    private static int checks;
    private static void check(boolean pass, String message) {
        checks++;
        if (!pass) throw new AssertionError(message);
    }
    private static byte[] join(byte[]... parts) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) out.write(part);
        return out.toByteArray();
    }
    private static byte[] number(int field, long value) throws IOException {
        return join(varint(field << 3), varint(value));
    }
    private static byte[] varint(long value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((value & ~0x7fL) != 0) { out.write((int) (value & 0x7f) | 0x80); value >>>= 7; }
        out.write((int) value); return out.toByteArray();
    }
    private static byte[] bytes(int field, byte[] data) throws IOException {
        return join(varint((field << 3) | 2), varint(data.length), data);
    }
    private static byte[] string(int field, String value) throws IOException { return bytes(field, value.getBytes(StandardCharsets.UTF_8)); }
    private static byte[] thread(int tid, String lib, String function) throws IOException {
        byte[] frame = join(number(1, 0x123), string(4, function), string(6, "/data/app/" + lib));
        return bytes(16, join(number(1, tid), bytes(2, join(number(1, tid), string(2, "worker"), bytes(4, frame)))));
    }
    public static void main(String[] args) throws Exception {
        byte[] tombstone = join(number(6, 42), bytes(10, join(number(1, 11), string(2, "SIGSEGV"))),
                string(14, "FATAL_TEST ticket=DUMMY_SECRET&lang=zh"), thread(7, "libwrong.so", "WrongThread"),
                thread(42, "libblackbox.so", "NativeFault"), bytes(17, string(7, "DO_NOT_EXPORT_MEMORY")));
        String summary = NativeCrashSummary.summarize(tombstone);
        check(summary.contains("SIGSEGV") && summary.contains("FATAL_TEST"), "signal and abort message decoded from official schema");
        check(summary.contains("libblackbox.so") && summary.contains("NativeFault") && summary.contains("0x123"), "faulting thread library, symbol and relative PC retained");
        check(!summary.contains("libwrong.so") && !summary.contains("WrongThread"), "unrelated thread cannot be mistaken for faulting thread");
        check(!summary.contains("DUMMY_SECRET") && !summary.contains("DO_NOT_EXPORT_MEMORY"), "secrets and memory fields not exported");
        check(summary.length() <= 260, "native clue has a bounded budget");
        check(NativeCrashSummary.summarize(join(thread(42, "libblackbox.so", "NativeFault"), number(6, 42))).contains("NativeFault"), "protobuf field ordering does not change fault-thread selection");
        check(NativeCrashSummary.summarize(new byte[]{(byte) 0x82}).contains("无法解析"), "truncated varint handled explicitly");
        check(NativeCrashSummary.summarize(new byte[]{(byte) 0x72, (byte) 0xff, (byte) 0x7f}).contains("无法解析"), "out-of-bounds string length rejected");
        check(NativeCrashSummary.summarize(new byte[0]).contains("未包含"), "empty proto does not invent native frames");
        byte[] large = new byte[NativeCrashSummary.MAX_BYTES + 1];
        check(NativeCrashSummary.summarize(large).contains("过大"), "oversized input rejected");
        check(NativeCrashSummary.summarize(new byte[]{0}).contains("无法解析"), "invalid protobuf tag rejected");
        System.out.println("PASS: " + checks + " native crash decode checks");
    }
}
