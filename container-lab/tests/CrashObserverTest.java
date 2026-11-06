package app.containerlab;

public final class CrashObserverTest {
    public static void main(String[] args) {
        final StringBuilder sequence = new StringBuilder();
        Throwable original = new IllegalStateException("TEST_CAUSE");
        Thread thread = Thread.currentThread();
        CrashObserver observer = new CrashObserver((t, e) -> {
            if (t != thread || e != original) throw new AssertionError("original exception identity");
            sequence.append('O');
        }, (t, e) -> {
            if (t != thread || e != original) throw new AssertionError("forward original exception");
            sequence.append('P');
        });
        observer.uncaughtException(thread, original);
        if (!"OP".contentEquals(sequence)) throw new AssertionError("observe before original handler");
        sequence.setLength(0);
        new CrashObserver((t,e) -> { throw new RuntimeException("logger failed"); }, (t,e) -> sequence.append('P')).uncaughtException(thread, original);
        if (!"P".contentEquals(sequence)) throw new AssertionError("logger failure cannot skip original handler");
        new CrashObserver((t,e) -> sequence.append('O'), null).uncaughtException(thread, original);
        if (!"PO".contentEquals(sequence)) throw new AssertionError("null platform handler supported");
        try {
            new CrashObserver((t,e) -> {}, (t,e) -> { throw new IllegalArgumentException("PLATFORM_FAILURE"); }).uncaughtException(thread, original);
            throw new AssertionError("do not swallow platform handler behavior");
        } catch (IllegalArgumentException expected) { }
        System.out.println("PASS: 6 pre-handler observer checks");
    }
}
