package app.containerlab;

import java.util.regex.Pattern;

final class DiagnosticRedactor {
    private static final Pattern SECRET = Pattern.compile("(?i)(\\b(?:ticket|token|access_token|refresh_token|session_key|code|phone|mobile|smscode)\\s*(?:=|%3d)\\s*)[^&\\s<>\"']+");
    static String redact(String message) {
        return message == null ? "" : SECRET.matcher(message).replaceAll("$1[redacted]");
    }
}
