package app.containerlab;

final class CrashObserver implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler observer, original;
    CrashObserver(Thread.UncaughtExceptionHandler observer, Thread.UncaughtExceptionHandler original) {
        this.observer = observer; this.original = original;
    }
    @Override public void uncaughtException(Thread thread, Throwable error) {
        try { observer.uncaughtException(thread, error); }
        catch (Throwable ignored) { /* Observation must not change the original handler's behavior. */ }
        if (original != null) original.uncaughtException(thread, error);
    }
}
