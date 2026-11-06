package app.fixedlocation;

/** Monotonic-clock health metrics; does not infer what another app accepted. */
public final class UpdateHealth {
    public enum Readback { MATCH, STALE, NON_MOCK, DIFFERENT, INVALID_TIME }
    private long count;
    private long last;
    private long maxGap;

    public void record(long elapsedMillis) {
        if (count > 0) maxGap = Math.max(maxGap, elapsedMillis - last);
        last = elapsedMillis;
        count++;
    }

    public long count() { return count; }
    public long maxGapMillis() { return maxGap; }
    public boolean isFresh(long now) { return count > 0 && isFresh(last, now); }
    public static boolean isFresh(long lastUpdate, long now) { return now >= lastUpdate && now - lastUpdate <= 5000; }

    public static Readback readback(long ageMillis, double distanceMeters, boolean mock) {
        if (ageMillis < 0) return Readback.INVALID_TIME;
        if (ageMillis > 5000) return Readback.STALE;
        if (!mock) return Readback.NON_MOCK;
        return distanceMeters <= 100 ? Readback.MATCH : Readback.DIFFERENT;
    }
}
