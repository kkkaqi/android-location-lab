package app.fixedlocation;

import java.util.*;

public class CoreTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static void rejects(String lat, String lon) {
        boolean rejected = false;
        try { Coordinates.parse(lat, lon); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "must reject: " + lat + ", " + lon);
    }
    private static class Backend implements MockSession.Backend {
        final Set<String> installed = new LinkedHashSet<>();
        final List<String> sent = new ArrayList<>();
        final List<String> removed = new ArrayList<>();
        String failInstall, failSend, failRemove;
        boolean authorizationDenied;
        public void ensureAuthorized() {
            if (authorizationDenied) throw new SecurityException("mock AppOp revoked");
        }
        public void install(String p) {
            if (authorizationDenied) return; // Android 15 silently ignores denied AppOps.
            if (p.equals(failInstall)) throw new SecurityException("install denied");
            installed.add(p);
        }
        public void send(String p, Coordinates c) {
            if (authorizationDenied) return;
            if (p.equals(failSend)) throw new SecurityException("send denied");
            sent.add(p);
        }
        public void remove(String p) {
            if (authorizationDenied) return;
            removed.add(p);
            if (p.equals(failRemove)) throw new SecurityException("remove denied");
            installed.remove(p);
        }
    }
    public static void main(String[] args) {
        Coordinates c = Coordinates.parse(" 39.9087 ", "116.3975");
        check(c.latitude == 39.9087 && c.longitude == 116.3975, "parse trimmed coordinates");
        check(Coordinates.parse("-90", "180").latitude == -90, "boundary inclusive");
        check(Coordinates.parse("90", "-180").longitude == -180, "boundary inclusive");
        rejects("90.01", "0"); rejects("-90.01", "0"); rejects("0", "180.01");
        rejects("0", "-180.01"); rejects("NaN", "0"); rejects("0", "Infinity");
        rejects("-Infinity", "0"); rejects("", "0"); rejects("a", "b"); rejects(null, "0");

        Backend b = new Backend();
        MockSession s = new MockSession(b, Arrays.asList("gps", "network", "fused"));
        s.start(c);
        check(b.sent.size() == 3 && s.providers().size() == 3, "start sends immediate fix");
        s.tick(); check(b.sent.size() == 6, "tick updates every provider");
        s.stop(); s.stop();
        check(b.installed.isEmpty() && b.removed.size() == 3, "stop idempotent and cleans all providers");
        s.tick(); check(b.sent.size() == 6, "tick after stop does nothing");

        Backend partial = new Backend(); partial.failInstall = "network";
        MockSession p = new MockSession(partial, Arrays.asList("gps", "network"));
        p.start(c);
        check(p.providers().equals(Collections.singletonList("gps")), "partial support remains usable");
        check(!p.warnings().isEmpty(), "partial failure is visible");
        p.stop();

        Backend firstFix = new Backend(); firstFix.failSend = "gps";
        MockSession failed = new MockSession(firstFix, Collections.singletonList("gps"));
        boolean denied = false;
        try { failed.start(c); } catch (IllegalStateException e) { denied = true; }
        check(denied && firstFix.installed.isEmpty(), "first fix failure rolls back and fails start");

        Backend later = new Backend();
        MockSession running = new MockSession(later, Arrays.asList("gps", "network"));
        running.start(c); later.failSend = "gps";
        boolean lost = false;
        try { running.tick(); } catch (IllegalStateException e) { lost = true; }
        check(lost && later.installed.isEmpty() && running.providers().isEmpty(), "runtime failure cleans and stops session");

        Backend cleanup = new Backend();
        MockSession dirty = new MockSession(cleanup, Arrays.asList("gps", "network"));
        dirty.start(c); cleanup.failRemove = "gps";
        dirty.stop();
        check(cleanup.removed.contains("network"), "cleanup continues after one removal fails");
        check(!dirty.warnings().isEmpty(), "cleanup failure surfaced");

        Backend revoked = new Backend();
        MockSession revokedSession = new MockSession(revoked, Collections.singletonList("gps"));
        revokedSession.start(c); revoked.authorizationDenied = true;
        boolean stopped = false;
        try { revokedSession.tick(); } catch (IllegalStateException e) { stopped = true; }
        check(stopped && revokedSession.providers().isEmpty(), "silent AppOp denial stops reported running state");
        check(!revokedSession.warnings().isEmpty(), "denied cleanup is reported, never claimed successful");

        Backend unauthorized = new Backend(); unauthorized.authorizationDenied = true;
        MockSession unauthorizedSession = new MockSession(unauthorized, Collections.singletonList("gps"));
        boolean startupDenied = false;
        try { unauthorizedSession.start(c); } catch (RuntimeException e) { startupDenied = true; }
        check(startupDenied && unauthorizedSession.providers().isEmpty(), "silent AppOp denial cannot report startup success");

        UpdateHealth health = new UpdateHealth();
        check(health.count() == 0 && !health.isFresh(1000), "no updates cannot appear healthy");
        health.record(1000); health.record(2000); health.record(12000);
        check(health.count() == 3 && health.maxGapMillis() == 10000, "records background scheduling interruptions");
        check(health.isFresh(13000) && !health.isFresh(18000), "stale heartbeat cannot appear current");
        check(UpdateHealth.readback(1000, 3, true) == UpdateHealth.Readback.MATCH, "fresh mock near target matches");
        check(UpdateHealth.readback(15000, 0, true) == UpdateHealth.Readback.STALE, "old target cache cannot imply ongoing injection");
        check(UpdateHealth.readback(500, 0, false) == UpdateHealth.Readback.NON_MOCK, "unmarked real data must be distinguished even at target");
        check(UpdateHealth.readback(500, 1000000, true) == UpdateHealth.Readback.DIFFERENT, "fresh wrong mock location reports conflict");
        check(UpdateHealth.readback(-1, 0, true) == UpdateHealth.Readback.INVALID_TIME, "future cache timestamp is not accepted as fresh");
        System.out.println("PASS: " + checks + " checks");
    }
}
