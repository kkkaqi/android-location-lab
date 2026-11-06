package app.containerlab;

import android.app.Application;
import android.content.Context;
import com.tegufy.blackbox.BlackBoxCore;
import com.tegufy.blackbox.app.BActivityThread;
import com.tegufy.blackbox.app.configuration.AppLifecycleCallback;
import com.tegufy.blackbox.app.configuration.ClientConfiguration;

public final class LabApplication extends Application {
    static volatile String initializationError;
    private static boolean isWechatGuest() {
        // Activity Context package names are deliberately rewritten to the host by the core.
        return BActivityThread.getAppConfig() != null && BActivityThread.getUserId() == 0
                && ImportPolicy.WECHAT.equals(BActivityThread.getAppPackageName());
    }
    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Thread.UncaughtExceptionHandler platformHandler = Thread.getDefaultUncaughtExceptionHandler();
        LabLog.append(base, "进程启动：pid=" + android.os.Process.myPid());
        try {
            BlackBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override public String getHostPackageName() { return base.getPackageName(); }
                @Override public String getLogSenderChatId() { return ""; }
                @Override public boolean isEnableDaemonService() { return false; }
                @Override public boolean isUseVpnNetwork() { return false; }
                @Override public boolean isHideRoot() { return false; }
                @Override public boolean isDisableFlagSecure() { return false; }
            });
            LabLog.append(base, "引擎 attach 调用已返回（不代表访客应用已启动）");
        } catch (Throwable error) {
            initializationError = "引擎初始化失败：" + error;
            LabLog.error(base, "attach", error);
        }
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            LabLog.error(base, "进程未捕获异常", error);
            if (platformHandler != null) platformHandler.uncaughtException(thread, error);
            else { android.os.Process.killProcess(android.os.Process.myPid()); System.exit(10); }
        });
        CrashEvidence.install(base);
    }
    @Override public void onCreate() {
        super.onCreate();
        if (initializationError != null) return;
        try {
            BlackBoxCore.get().doCreate();
            BlackBoxCore.get().addAppLifecycleCallback(new AppLifecycleCallback() {
                @Override public void beforeCreateApplication(String packageName, String processName, Context context, int userId) {
                    RendererFrameCompat.beforeApplication(packageName, processName, userId, LabApplication.this);
                }
                @Override public void onActivityCreated(android.app.Activity activity, android.os.Bundle state) {
                    if (isWechatGuest()) {
                        LabLog.append(LabApplication.this, "页面创建：" + activity.getClass().getName());
                        GuestWebProbe.onCreated(activity, LabApplication.this);
                    }
                }
                @Override public void onActivityResumed(android.app.Activity activity) {
                    if (isWechatGuest())
                        LabLog.append(LabApplication.this, "页面恢复：" + activity.getClass().getName());
                }
                @Override public void afterApplicationOnCreate(String packageName, String processName, Application app, int userId) {
                    if (ImportPolicy.WECHAT.equals(packageName)) LabLog.append(LabApplication.this, "访客 Application 回调已返回：process=" + processName + "，class=" + app.getClass().getName() + "。不代表微信真实初始化或页面可用。");
                    if (ImportPolicy.WECHAT.equals(packageName)) {
                        com.tegufy.blackbox.fake.service.ILocationManagerProxy.fixContext(app);
                        if (packageName.equals(processName)) {
                            try {
                                LabLog.append(LabApplication.this, "访客相机权限：guest=" + app.checkSelfPermission(android.Manifest.permission.CAMERA)
                                        + "，host=" + LabApplication.this.checkSelfPermission(android.Manifest.permission.CAMERA)
                                        + "（0=系统允许；不代表预览或认证通过）");
                            } catch (Throwable error) { LabLog.error(LabApplication.this, "访客相机权限检查", error); }
                        }
                        try {
                            int internet = app.checkSelfPermission(android.Manifest.permission.INTERNET);
                            int networkState = app.checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE);
                            LabLog.append(LabApplication.this, "访客 Context 联网权限：INTERNET=" + internet + "，ACCESS_NETWORK_STATE=" + networkState + "（0=系统确认授予）；不代表安全验证已完成。");
                        } catch (Throwable error) { LabLog.error(LabApplication.this, "访客联网权限检查", error); }
                        GuestLocationCheck.run(app,LabApplication.this);
                    }
                }
            });
            LabLog.append(this, "引擎 create 调用已返回");
        } catch (Throwable error) {
            initializationError = "引擎 create 失败：" + error;
            LabLog.error(this, "create", error);
        }
    }
}
