package app.fixedlocation;

import java.util.ArrayList;
import java.util.List;

/** Owns providers installed by this session, including partial-start rollback. */
public final class MockSession {
    public interface Backend {
        void ensureAuthorized();
        void install(String provider);
        void send(String provider, Coordinates coordinates);
        void remove(String provider);
    }

    private final Backend backend;
    private final List<String> candidates;
    private final List<String> active = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private Coordinates coordinates;

    public MockSession(Backend backend, List<String> candidates) {
        this.backend = backend;
        this.candidates = new ArrayList<>(candidates);
    }

    public void start(Coordinates coordinates) {
        if (!active.isEmpty()) throw new IllegalStateException("请先停止当前定位");
        this.coordinates = coordinates;
        warnings.clear();
        backend.ensureAuthorized();
        for (String provider : candidates) {
            boolean installed = false;
            try {
                backend.install(provider);
                installed = true;
                backend.send(provider, coordinates);
                active.add(provider);
            } catch (RuntimeException e) {
                warnings.add(provider + "：" + reason(e));
                if (installed) remove(provider);
            }
        }
        if (active.isEmpty()) throw new IllegalStateException("无法注入模拟位置。请在开发者选项中选择本应用。" + String.join("；", warnings));
    }

    public void tick() {
        if (active.isEmpty()) return;
        try {
            backend.ensureAuthorized();
            for (String provider : active) backend.send(provider, coordinates);
        } catch (RuntimeException e) {
            stop();
            throw new IllegalStateException("位置更新失败，已停止。" + reason(e), e);
        }
    }

    public void stop() {
        for (String provider : new ArrayList<>(active)) remove(provider);
        active.clear();
    }

    private void remove(String provider) {
        try { backend.ensureAuthorized(); backend.remove(provider); }
        catch (RuntimeException e) { warnings.add("清理 " + provider + " 失败：" + reason(e) + "。请在开发者选项取消模拟应用或重启手机。"); }
    }

    private static String reason(RuntimeException error) {
        if (error instanceof SecurityException) return "模拟定位授权不足或已被撤销";
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    public List<String> providers() { return new ArrayList<>(active); }
    public List<String> warnings() { return new ArrayList<>(warnings); }
}
