package com.tegufy.blackbox.fake.hook;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.tegufy.blackbox.BlackBoxCore;
import com.tegufy.blackbox.fake.delegate.AppInstrumentation;

import com.tegufy.blackbox.fake.service.HCallbackProxy;
import com.tegufy.blackbox.fake.service.IAccessibilityManagerProxy;
import com.tegufy.blackbox.fake.service.IAccountManagerProxy;
import com.tegufy.blackbox.fake.service.IActivityClientProxy;
import com.tegufy.blackbox.fake.service.IActivityManagerProxy;
import com.tegufy.blackbox.fake.service.IActivityTaskManagerProxy;
import com.tegufy.blackbox.fake.service.IAlarmManagerProxy;
import com.tegufy.blackbox.fake.service.IAppOpsManagerProxy;
import com.tegufy.blackbox.fake.service.IAppWidgetManagerProxy;
import com.tegufy.blackbox.fake.service.IAttributionSourceProxy;
import com.tegufy.blackbox.fake.service.IAutofillManagerProxy;
import com.tegufy.blackbox.fake.service.ISensitiveContentProtectionManagerProxy;
import com.tegufy.blackbox.fake.service.ISettingsSystemProxy;
import com.tegufy.blackbox.fake.service.IConnectivityManagerProxy;
import com.tegufy.blackbox.fake.service.ISystemSensorManagerProxy;
import com.tegufy.blackbox.fake.service.IContentProviderProxy;
import com.tegufy.blackbox.fake.service.IXiaomiAttributionSourceProxy;
import com.tegufy.blackbox.fake.service.IXiaomiSettingsProxy;
import com.tegufy.blackbox.fake.service.IXiaomiMiuiServicesProxy;
import com.tegufy.blackbox.fake.service.IDnsResolverProxy;
import com.tegufy.blackbox.fake.service.IContextHubServiceProxy;
import com.tegufy.blackbox.fake.service.IDeviceIdentifiersPolicyProxy;
import com.tegufy.blackbox.fake.service.IDevicePolicyManagerProxy;
import com.tegufy.blackbox.fake.service.IDisplayManagerProxy;
import com.tegufy.blackbox.fake.service.IFingerprintManagerProxy;
import com.tegufy.blackbox.fake.service.IGraphicsStatsProxy;
import com.tegufy.blackbox.fake.service.IJobServiceProxy;
import com.tegufy.blackbox.fake.service.ILauncherAppsProxy;
import com.tegufy.blackbox.fake.service.ILocationManagerProxy;
import com.tegufy.blackbox.fake.service.IMediaRouterServiceProxy;
import com.tegufy.blackbox.fake.service.IMediaSessionManagerProxy;
import com.tegufy.blackbox.fake.service.IAudioServiceProxy;
import com.tegufy.blackbox.fake.service.ISensorPrivacyManagerProxy;
import com.tegufy.blackbox.fake.service.ContentResolverProxy;
import com.tegufy.blackbox.fake.service.IWebViewUpdateServiceProxy;
import com.tegufy.blackbox.fake.service.IMiuiSecurityManagerProxy;
import com.tegufy.blackbox.fake.service.SystemLibraryProxy;
import com.tegufy.blackbox.fake.service.ReLinkerProxy;
import com.tegufy.blackbox.fake.service.WebViewProxy;
import com.tegufy.blackbox.fake.service.WebViewFactoryProxy;
import com.tegufy.blackbox.fake.service.MediaRecorderProxy;
import com.tegufy.blackbox.fake.service.AudioRecordProxy;
import com.tegufy.blackbox.fake.service.MediaRecorderClassProxy;
import com.tegufy.blackbox.fake.service.SQLiteDatabaseProxy;
import com.tegufy.blackbox.fake.service.ClassLoaderProxy;
import com.tegufy.blackbox.fake.service.FileSystemProxy;
import com.tegufy.blackbox.fake.service.GmsProxy;
import com.tegufy.blackbox.fake.service.LevelDbProxy;
import com.tegufy.blackbox.fake.service.DeviceIdProxy;
import com.tegufy.blackbox.fake.service.GoogleAccountManagerProxy;
import com.tegufy.blackbox.fake.service.GooglePlayServicesProxy;
import com.tegufy.blackbox.fake.service.AuthenticationProxy;
import com.tegufy.blackbox.fake.service.AndroidIdProxy;
import com.tegufy.blackbox.fake.service.AudioPermissionProxy;

import com.tegufy.blackbox.fake.service.INetworkManagementServiceProxy;
import com.tegufy.blackbox.fake.service.INotificationManagerProxy;
import com.tegufy.blackbox.fake.service.IPackageManagerProxy;
import com.tegufy.blackbox.fake.service.IPermissionManagerProxy;
import com.tegufy.blackbox.fake.service.IPersistentDataBlockServiceProxy;
import com.tegufy.blackbox.fake.service.IPhoneSubInfoProxy;
import com.tegufy.blackbox.fake.service.IPowerManagerProxy;
import com.tegufy.blackbox.fake.service.ApkAssetsProxy;
import com.tegufy.blackbox.fake.service.ResourcesManagerProxy;
import com.tegufy.blackbox.fake.service.IShortcutManagerProxy;
import com.tegufy.blackbox.fake.service.IStorageManagerProxy;
import com.tegufy.blackbox.fake.service.IStorageStatsManagerProxy;
import com.tegufy.blackbox.fake.service.ISystemUpdateProxy;
import com.tegufy.blackbox.fake.service.ITelephonyManagerProxy;
import com.tegufy.blackbox.fake.service.ITelephonyRegistryProxy;
import com.tegufy.blackbox.fake.service.IUserManagerProxy;
import com.tegufy.blackbox.fake.service.IVibratorServiceProxy;
import com.tegufy.blackbox.fake.service.IVpnManagerProxy;
import com.tegufy.blackbox.fake.service.IWifiManagerProxy;
import com.tegufy.blackbox.fake.service.IWifiScannerProxy;
import com.tegufy.blackbox.fake.service.IWindowManagerProxy;
import com.tegufy.blackbox.fake.service.context.ContentServiceStub;
import com.tegufy.blackbox.fake.service.context.RestrictionsManagerStub;
import com.tegufy.blackbox.fake.service.libcore.OsStub;
import com.tegufy.blackbox.utils.Slog;
import com.tegufy.blackbox.utils.compat.BuildCompat;
import com.tegufy.blackbox.fake.service.ISettingsProviderProxy;
import com.tegufy.blackbox.fake.service.FeatureFlagUtilsProxy;
import com.tegufy.blackbox.fake.service.WorkManagerProxy;



public class HookManager {
    public static final String TAG = "HookManager";

    private static final HookManager sHookManager = new HookManager();

    private final Map<Class<?>, IInjectHook> mInjectors = new HashMap<>();

    public static HookManager get() {
        return sHookManager;
    }

    /**
     * Check if Google Mobile Services support is available on this device
     */
    private static boolean isGmsSupportAvailable() {
        try {
            // Check if GMS package exists on the device
            BlackBoxCore.getPackageManager().getPackageInfo("com.google.android.gms", 0);
            Slog.d(TAG, "GMS support available on device");
            return true;
        } catch (Throwable e) {
            Slog.d(TAG, "GMS not available on device, skipping GMS proxies");
            return false;
        }
    }

    public void init() {
        if (BlackBoxCore.get().isBlackProcess() || BlackBoxCore.get().isServerProcess()) {
            addInjector(new IDisplayManagerProxy());
            addInjector(new OsStub());
            addInjector(new IActivityManagerProxy());
            addInjector(new IPackageManagerProxy());
            if (android.os.Build.VERSION.SDK_INT >= 33) addInjector(new com.tegufy.blackbox.fake.service.ILocaleManagerProxy());
            addInjector(new ITelephonyManagerProxy());
            addInjector(new HCallbackProxy());
            addInjector(new IAppOpsManagerProxy());
            addInjector(new INotificationManagerProxy());
            addInjector(new IAlarmManagerProxy());
            addInjector(new IAppWidgetManagerProxy());
            addInjector(new ContentServiceStub());
            addInjector(new IWindowManagerProxy());
            addInjector(new IUserManagerProxy());
            addInjector(new RestrictionsManagerStub());
            addInjector(new IMediaSessionManagerProxy());
            addInjector(new IAudioServiceProxy());
            addInjector(new ISensorPrivacyManagerProxy());
            addInjector(new ContentResolverProxy());
            addInjector(new IWebViewUpdateServiceProxy());
            addInjector(new SystemLibraryProxy());
            addInjector(new ReLinkerProxy());
            addInjector(new WebViewProxy());
            addInjector(new WebViewFactoryProxy());
            addInjector(new WorkManagerProxy());
            addInjector(new MediaRecorderProxy());
            addInjector(new AudioRecordProxy());
            addInjector(new IMiuiSecurityManagerProxy());
            addInjector(new ISettingsProviderProxy());
            addInjector(new FeatureFlagUtilsProxy());
            addInjector(new MediaRecorderClassProxy());
            addInjector(new SQLiteDatabaseProxy());
            addInjector(new ClassLoaderProxy());
            addInjector(new FileSystemProxy());
            
            // Only add GMS-related proxies if GMS support is available
            if (isGmsSupportAvailable()) {
                addInjector(new GmsProxy());
                addInjector(new GoogleAccountManagerProxy());
                addInjector(new GooglePlayServicesProxy());
                addInjector(new AuthenticationProxy());
            }
            
            addInjector(new LevelDbProxy());
            addInjector(new DeviceIdProxy());
            addInjector(new AndroidIdProxy());
            addInjector(new AudioPermissionProxy());
            addInjector(new ILocationManagerProxy());
            addInjector(new IStorageManagerProxy());
            addInjector(new ILauncherAppsProxy());
            addInjector(new IJobServiceProxy());
            addInjector(new IAccessibilityManagerProxy());
            addInjector(new ITelephonyRegistryProxy());
            addInjector(new IDevicePolicyManagerProxy());
            addInjector(new IAccountManagerProxy());
            addInjector(new IConnectivityManagerProxy());
            addInjector(new IDnsResolverProxy());
                    addInjector(new IAttributionSourceProxy());
        addInjector(new IContentProviderProxy());
        addInjector(new ISettingsSystemProxy());
        addInjector(new ISystemSensorManagerProxy());
        
        
        addInjector(new IXiaomiAttributionSourceProxy());
        addInjector(new IXiaomiSettingsProxy());
        addInjector(new IXiaomiMiuiServicesProxy());
            addInjector(new IPhoneSubInfoProxy());
            addInjector(new IMediaRouterServiceProxy());
            addInjector(new IPowerManagerProxy());
            addInjector(new IContextHubServiceProxy());
            
            addInjector(new IVibratorServiceProxy());
            addInjector(new IPersistentDataBlockServiceProxy());
            addInjector(AppInstrumentation.get());
            
            addInjector(new IWifiManagerProxy());
            addInjector(new IWifiScannerProxy());
            addInjector(new ApkAssetsProxy());
            addInjector(new ResourcesManagerProxy());
            
            if (BuildCompat.isS()) {
                addInjector(new IActivityClientProxy(null));
                addInjector(new IVpnManagerProxy());
            }
            
            if (BuildCompat.isS()) {
                addInjector(new ISensitiveContentProtectionManagerProxy());
            }
            
            if (BuildCompat.isR()) {
                addInjector(new IPermissionManagerProxy());
            }
            
            if (BuildCompat.isQ()) {
                addInjector(new IActivityTaskManagerProxy());
            }
            
            if (BuildCompat.isPie()) {
                addInjector(new ISystemUpdateProxy());
            }
            
            if (BuildCompat.isOreo()) {
                addInjector(new IAutofillManagerProxy());
                addInjector(new IDeviceIdentifiersPolicyProxy());
                addInjector(new IStorageStatsManagerProxy());
            }
            
            if (BuildCompat.isN_MR1()) {
                addInjector(new IShortcutManagerProxy());
            }
            
            if (BuildCompat.isN()) {
                addInjector(new INetworkManagementServiceProxy());
            }
            
            if (BuildCompat.isM()) {
                addInjector(new IFingerprintManagerProxy());
                addInjector(new IGraphicsStatsProxy());
            }
            
            if (BuildCompat.isL()) {
                addInjector(new IJobServiceProxy());
            }
        }
        injectAll();
    }

    public void checkEnv(Class<?> clazz) {
        IInjectHook iInjectHook = mInjectors.get(clazz);
        if (iInjectHook != null && iInjectHook.isBadEnv()) {
            Log.d(TAG, "checkEnv: " + clazz.getSimpleName() + " is bad env");
            iInjectHook.injectHook();
        }
    }

    public void checkAll() {
        for (Class<?> aClass : mInjectors.keySet()) {
            IInjectHook iInjectHook = mInjectors.get(aClass);
            if (iInjectHook != null && iInjectHook.isBadEnv()) {
                Log.d(TAG, "checkEnv: " + aClass.getSimpleName() + " is bad env");
                iInjectHook.injectHook();
            }
        }
    }

    void addInjector(IInjectHook injectHook) {
        mInjectors.put(injectHook.getClass(), injectHook);
    }

    void injectAll() {
        for (IInjectHook value : mInjectors.values()) {
            try {
                Slog.d(TAG, "hook: " + value);
                value.injectHook();
            } catch (Throwable e) {
                Slog.d(TAG, "hook error: " + value);
                
                handleHookError(value, e);
            }
        }
    }

    
    private void handleHookError(IInjectHook hook, Throwable e) {
        String hookName = hook.getClass().getSimpleName();
        
        
        Slog.e(TAG, "Hook failed: " + hookName + " - " + e.getMessage(), e);
        
        
        if (hookName.contains("ActivityManager") || 
            hookName.contains("PackageManager") ||
            hookName.contains("WebView") ||
            hookName.contains("ContentProvider")) {
            
            Slog.w(TAG, "Critical hook failed: " + hookName + ", attempting recovery");
            
            try {
                
                if (hook.isBadEnv()) {
                    Slog.d(TAG, "Attempting to recover hook: " + hookName);
                    hook.injectHook();
                }
            } catch (Throwable recoveryException) {
                Slog.e(TAG, "Hook recovery failed: " + hookName, recoveryException);
            }
        }
    }

    
    public boolean areCriticalHooksInstalled() {
        String[] criticalHooks = {
            "IActivityManagerProxy",
            "IPackageManagerProxy", 
            "WebViewProxy",
            "IContentProviderProxy"
        };
        
        for (String hookName : criticalHooks) {
            boolean found = false;
            for (Class<?> hookClass : mInjectors.keySet()) {
                if (hookClass.getSimpleName().equals(hookName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Slog.w(TAG, "Critical hook missing: " + hookName);
                return false;
            }
        }
        
        Slog.d(TAG, "All critical hooks are installed");
        return true;
    }

    
    public void reinitializeHooks() {
        Slog.d(TAG, "Reinitializing all hooks");
        
        
        mInjectors.clear();
        
        
        init();
        
        Slog.d(TAG, "Hook reinitialization completed");
    }
}
