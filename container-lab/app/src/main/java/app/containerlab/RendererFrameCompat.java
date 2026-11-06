package app.containerlab;

import android.content.Context;
import android.os.Build;
import android.view.Choreographer;
import com.tegufy.blackbox.app.BActivityThread;
import java.lang.reflect.Field;

/** Opt out of optional OEM scheduling in a guest renderer, leaving Chromium's sandbox intact. */
final class RendererFrameCompat {
    static void beforeApplication(String packageName, String processName, int userId, Context host) {
        if (!RendererCompatPolicy.applies(BActivityThread.getAppConfig() != null, userId,
                packageName, processName, Build.VERSION.SDK_INT, Build.MANUFACTURER)) return;
        try {
            Class<?> feature = Class.forName("com.oplus.uifirst.IOplusUIFirstManager");
            Class<?> common = Class.forName("android.common.IOplusCommonFeature");
            Class<?> cache = Class.forName("android.common.OplusFeatureCache");
            Object defaults = feature.getField("DEFAULT").get(null);
            cache.getMethod("set", common).invoke(null, defaults);
            if (cache.getMethod("get", common).invoke(null, defaults) != defaults)
                throw new IllegalStateException("OEM调度缓存回读不符");
            // Host startup may already have created the main Choreographer with the real feature.
            // Update that one process-local instance before the renderer enables seccomp.
            Choreographer choreographer = Choreographer.getInstance();
            int updated = 0;
            for (Field field : Choreographer.class.getDeclaredFields()) {
                if (!"android.view.IChoreographerExt".equals(field.getType().getName())) continue;
                field.setAccessible(true);
                Object extension = field.get(choreographer);
                if (extension == null) continue;
                Field manager = extension.getClass().getDeclaredField("mUIFirstMgr");
                manager.setAccessible(true);
                manager.set(extension, defaults);
                if (manager.get(extension) != defaults) throw new IllegalStateException("已有帧调度回读不符");
                updated++;
            }
            LabLog.append(host, "渲染兼容：已采用系统默认帧调度；已有编舞器=" + updated + "；未改变沙箱");
            if ((host.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() ->
                        choreographer.postFrameCallback(frameTime -> android.util.Log.i("ContainerLab",
                                "渲染兼容：延后帧回调已执行（仅调试）")), 10000);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            LabLog.error(host, "渲染帧调度适配失败", error);
        }
    }
}
