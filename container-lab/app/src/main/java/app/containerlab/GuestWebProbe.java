package app.containerlab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.view.View;
import com.tegufy.blackbox.BlackBoxCore;
import com.tegufy.blackbox.app.BActivityThread;

/** Fixed local rendering experiment, available only in this debuggable test host. */
final class GuestWebProbe {
    static final String HOST_REQUEST = "lab_public_web_probe";
    private static final String MARKER = "app.containerlab.LOCAL_WEB_PROBE";
    private static final String PUBLIC_URL = "https://example.com/";
    private static final String WEB_ACTIVITY = "com.tencent.mm.plugin.webview.ui.tools.MMWebViewUI";

    private static boolean debuggable(Context host) {
        return (host.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    static void launch(Context host) throws Exception {
        if (!debuggable(host)) throw new IllegalStateException("本地网页自检仅供调试构建");
        if (!BlackBoxCore.getBPackageManager().getService().isInstalled(ImportPolicy.WECHAT, 0))
            throw new IllegalStateException("请先导入微信");
        Intent intent = new Intent().setComponent(new ComponentName(ImportPolicy.WECHAT, WEB_ACTIVITY))
                .setAction(MARKER + "." + android.os.SystemClock.elapsedRealtimeNanos())
                .putExtra("rawUrl", PUBLIC_URL).putExtra(MARKER, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        BlackBoxCore.getBActivityManager().getService().startActivity(intent, 0);
        LabLog.append(host, "网页自检：已请求固定本地测试页；不代表微信登录成功");
    }

    static void onCreated(Activity activity, Context host) {
        if (!debuggable(host) || BActivityThread.getAppConfig() == null
                || BActivityThread.getUserId() != 0
                || !ImportPolicy.WECHAT.equals(BActivityThread.getAppPackageName())
                || !WEB_ACTIVITY.equals(activity.getClass().getName())) return;
        Intent intent = activity.getIntent();
        if (intent == null || !intent.getBooleanExtra(MARKER, false)
                || !PUBLIC_URL.equals(intent.getStringExtra("rawUrl"))) return;
        try {
            // Use the actual widget's SDK class, rather than resolving an SDK copy through Context.
            Object original = null;
            for (Class<?> owner = activity.getClass(); owner != null && original == null; owner = owner.getSuperclass()) {
                try {
                    java.lang.reflect.Field field = owner.getDeclaredField("f");
                    field.setAccessible(true);
                    Object value = field.get(activity);
                    if (value instanceof View) original = value;
                } catch (NoSuchFieldException ignored) { }
            }
            if (original == null) throw new IllegalStateException("当前微信版本没有可识别的网页控件");
            Class<?> type = original.getClass();
            while (type != null && !"com.tencent.xweb.WebView".equals(type.getName())) type = type.getSuperclass();
            if (type == null) throw new IllegalStateException("当前控件不属于XWeb");
            // Reuse the page-owned widget so XWeb initialization and destruction remain with WeChat.
            View web = (View) original;
            if (web.getParent() instanceof android.view.ViewGroup)
                ((android.view.ViewGroup) web.getParent()).removeView(web);
            LabLog.append(host, "网页自检：SDK类=" + type.getName() + "；使用当前页面控件");
            Class<?> sdkType = type;
            activity.setContentView(web);
            String html = "<html><head><title>Container local web test</title>"
                    + "<meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                    + "<body style='font:20px sans-serif;padding:32px;background:white;color:#111'>"
                    + "<h2>容器本地网页测试</h2><p>此页面仅测试渲染，不执行登录或安全验证。</p>"
                    + "<div style='width:48px;height:48px;border:6px solid #ddd;border-top-color:#168c64;"
                    + "border-radius:50%;animation:r 1s linear infinite'></div>"
                    + "<style>@keyframes r{to{transform:rotate(360deg)}}</style></body></html>";
            type.getMethod("loadDataWithBaseURL", String.class, String.class, String.class, String.class, String.class)
                    .invoke(web, "https://example.invalid/", html, "text/html", "UTF-8", null);
            activity.getWindow().getDecorView().postDelayed(() -> {
                if (activity.isDestroyed()) return;
                try {
                    Object progress = sdkType.getMethod("getProgress").invoke(web);
                    Object height = sdkType.getMethod("getContentHeight").invoke(web);
                    LabLog.append(host, "网页自检：本地页 progress=" + progress + " height=" + height + "；仅渲染检查");
                } catch (Exception error) { LabLog.error(host, "本地网页读回", error); }
            }, 2500);
        } catch (Exception error) { LabLog.error(host, "本地网页创建", error); }
    }
}
