package app.containerlab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded reader for selected AOSP Android15 tombstone.proto fields; never exports memory or logs. */
final class NativeCrashSummary {
    static final int MAX_BYTES = 4 * 1024 * 1024;
    static String summarize(byte[] data) {
        if (data == null || data.length == 0) return "原生记录未包含可用信号或故障栈";
        if (data.length > MAX_BYTES) return "原生记录过大，未解析";
        try {
            Cursor root = new Cursor(data, 0, data.length);
            int tid = 0;
            String signal = "", abort = "";
            List<Cursor> threads = new ArrayList<>();
            while (root.next()) {
                switch (root.field) {
                    case 6: tid = (int) root.integer(); break;
                    case 10: {
                        Cursor value = root.message();
                        int number = 0;
                        while (value.next()) {
                            if (value.field == 1) number = (int) value.integer();
                            else if (value.field == 2) signal = value.text(64);
                            else value.skip();
                        }
                        if (signal.isEmpty() && number != 0) signal = "signal=" + number;
                        break;
                    }
                    case 14: abort = root.text(4096); break;
                    case 16: {
                        Cursor thread = root.message();
                        if (threads.size() < 4096) threads.add(thread);
                        break;
                    }
                    default: root.skip();
                }
            }
            List<String> frames = new ArrayList<>();
            for (Cursor entry : threads) {
                int key = 0;
                Cursor value = null;
                while (entry.next()) {
                    if (entry.field == 1) key = (int) entry.integer();
                    else if (entry.field == 2) value = entry.message();
                    else entry.skip();
                }
                if (tid > 0 && key == tid && value != null) {
                    frames = frames(value);
                    break;
                }
            }
            StringBuilder out = new StringBuilder();
            if (!signal.isEmpty()) out.append(clean(signal, 24));
            if (!abort.isEmpty()) out.append(" abort=").append(clean(abort, 65));
            if (!frames.isEmpty()) {
                // Include the top frame and the first application/container frame when runtime abort frames precede it.
                String first = frames.get(0), useful = "";
                for (String frame : frames) {
                    if (!isRuntimeFrame(frame)) { useful = frame; break; }
                }
                out.append(" | ").append(clean(first, 75));
                if (!useful.isEmpty() && !useful.equals(first)) out.append(" | ").append(clean(useful, 80));
            }
            return out.length() == 0 ? "原生记录未包含可用信号或故障栈" : clean(out.toString(), 260);
        } catch (IOException | RuntimeException error) {
            return "原生记录无法解析（" + error.getClass().getSimpleName() + "）";
        }
    }

    private static boolean isRuntimeFrame(String frame) {
        return frame.startsWith("libc.so+") || frame.startsWith("libart.so+") || frame.startsWith("libbase.so+")
                || frame.startsWith("liblog.so+") || frame.startsWith("libutils.so+");
    }
    private static List<String> frames(Cursor thread) throws IOException {
        List<String> result = new ArrayList<>();
        while (thread.next()) {
            if (thread.field != 4 || result.size() >= 32) { thread.skip(); continue; }
            Cursor frame = thread.message();
            long pc = 0;
            String file = "", function = "";
            while (frame.next()) {
                if (frame.field == 1) pc = frame.integer();
                else if (frame.field == 4) function = frame.text(512);
                else if (frame.field == 6) file = frame.text(512);
                else frame.skip();
            }
            int slash = file.lastIndexOf('/');
            if (slash >= 0) file = file.substring(slash + 1);
            result.add((file.isEmpty() ? "未知库" : clean(file, 32)) + "+0x" + Long.toHexString(pc)
                    + (function.isEmpty() ? "" : "/" + clean(function, 40)));
        }
        return result;
    }

    private static String clean(String text, int max) {
        text = DiagnosticRedactor.redact(text).replaceAll("\\s+", " ").trim();
        if (text.length() <= max) return text;
        int end = max - 1;
        if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end) + "…";
    }

    private static final class Cursor {
        final byte[] data;
        final int end;
        int pos, field, wire;
        Cursor(byte[] data, int start, int end) { this.data = data; this.pos = start; this.end = end; }
        boolean next() throws IOException {
            if (pos == end) return false;
            long tag = varint();
            if (tag <= 0 || tag > Integer.MAX_VALUE) throw new IOException("invalid tag");
            field = (int) tag >>> 3; wire = (int) tag & 7;
            if (field == 0) throw new IOException("zero field");
            return true;
        }
        long integer() throws IOException { require(0); return varint(); }
        Cursor message() throws IOException {
            require(2);
            int count = length();
            Cursor result = new Cursor(data, pos, pos + count);
            pos += count; return result;
        }
        String text(int limit) throws IOException {
            require(2);
            int count = length();
            String text = new String(data, pos, Math.min(count, limit), StandardCharsets.UTF_8);
            pos += count; return text;
        }
        void skip() throws IOException {
            switch (wire) {
                case 0: varint(); return;
                case 1: advance(8); return;
                case 2: advance(length()); return;
                case 5: advance(4); return;
                default: throw new IOException("unsupported wire type");
            }
        }
        private void require(int expected) throws IOException { if (wire != expected) throw new IOException("wrong wire type"); }
        private int length() throws IOException {
            long count = varint();
            if (count < 0 || count > end - pos) throw new IOException("invalid length");
            return (int) count;
        }
        private void advance(int count) throws IOException {
            if (count < 0 || count > end - pos) throw new IOException("truncated field");
            pos += count;
        }
        private long varint() throws IOException {
            long value = 0;
            for (int i = 0; i < 10; i++) {
                if (pos >= end) throw new IOException("truncated varint");
                int b = data[pos++] & 255;
                if (i == 9 && (b & 254) != 0) throw new IOException("varint overflow");
                value |= (long) (b & 127) << (7 * i);
                if ((b & 128) == 0) return value;
            }
            throw new IOException("invalid varint");
        }
    }
}
