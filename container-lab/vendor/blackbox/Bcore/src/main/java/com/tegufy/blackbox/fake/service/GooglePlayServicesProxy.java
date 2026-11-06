package com.tegufy.blackbox.fake.service;

import android.content.Context;

import java.lang.reflect.Method;

import com.tegufy.blackbox.BlackBoxCore;
import com.tegufy.blackbox.core.GmsCore;
import com.tegufy.blackbox.fake.hook.ClassInvocationStub;
import com.tegufy.blackbox.fake.hook.MethodHook;
import com.tegufy.blackbox.fake.hook.ProxyMethod;
import com.tegufy.blackbox.utils.Slog;

/**
 * Proxy for Google Play Services availability checks
 * Makes apps think GMS is available even in virtual environment
 */
public class GooglePlayServicesProxy extends ClassInvocationStub {
    public static final String TAG = "GooglePlayServicesProxy";
    
    // Google Play Services status codes
    private static final int SUCCESS = 0;
    private static final int SERVICE_MISSING = 1;
    private static final int SERVICE_VERSION_UPDATE_REQUIRED = 2;
    private static final int SERVICE_DISABLED = 3;
    private static final int SERVICE_INVALID = 9;
    
    private static Object sGoogleApiAvailabilityInstance;

    public GooglePlayServicesProxy() {
        super();
        onlyProxy(true); // Don't try to inject, just proxy method calls
    }

    @Override
    protected Object getWho() {
        try {
            // Get GoogleApiAvailability.getInstance()
            Class<?> gmsClass = Class.forName("com.google.android.gms.common.GoogleApiAvailability");
            Method getInstanceMethod = gmsClass.getMethod("getInstance");
            sGoogleApiAvailabilityInstance = getInstanceMethod.invoke(null);
            if (sGoogleApiAvailabilityInstance != null) {
                Slog.d(TAG, "Successfully obtained GoogleApiAvailability instance for proxying");
                return sGoogleApiAvailabilityInstance;
            }
        } catch (Throwable e) {
            Slog.d(TAG, "GoogleApiAvailability class not found (GMS not in classpath)");
        }
        
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // Hook into the GoogleApiAvailability singleton
        // This is tricky because we need to replace the instance returned by getInstance()
        try {
            Class<?> gmsClass = Class.forName("com.google.android.gms.common.GoogleApiAvailability");
            
            // Try to replace the singleton instance via reflection
            java.lang.reflect.Field instanceField = null;
            try {
                instanceField = gmsClass.getDeclaredField("zaa"); // Obfuscated field name
            } catch (NoSuchFieldException e) {
                try {
                    instanceField = gmsClass.getDeclaredField("sInstance");
                } catch (NoSuchFieldException e2) {
                    Slog.d(TAG, "Could not find instance field in GoogleApiAvailability");
                }
            }
            
            if (instanceField != null) {
                instanceField.setAccessible(true);
                instanceField.set(null, proxyInvocation);
                Slog.d(TAG, "Successfully injected proxy into GoogleApiAvailability singleton");
            }
        } catch (Throwable e) {
            Slog.d(TAG, "Could not inject GoogleApiAvailability proxy: " + e.getMessage());
        }
    }

    @Override
    public boolean isBadEnv() {
        // Check if the singleton needs to be re-injected
        try {
            if (sGoogleApiAvailabilityInstance == null) {
                return false; // Not initialized yet
            }
            
            Class<?> gmsClass = Class.forName("com.google.android.gms.common.GoogleApiAvailability");
            Method getInstanceMethod = gmsClass.getMethod("getInstance");
            Object currentInstance = getInstanceMethod.invoke(null);
            
            // If current instance is not our proxy, environment is bad
            return currentInstance != getProxyInvocation();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Hook isGooglePlayServicesAvailable to return SUCCESS when GMS is installed in virtual space
     */
    @ProxyMethod("isGooglePlayServicesAvailable")
    public static class IsGooglePlayServicesAvailable extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Context context = null;
                if (args != null && args.length > 0 && args[0] instanceof Context) {
                    context = (Context) args[0];
                }
                
                // Check if GMS is installed in current virtual user
                int userId = BlackBoxCore.get().getUserId();
                boolean gmsInstalled = GmsCore.isInstalledGoogleService(userId);
                
                if (gmsInstalled) {
                    Slog.d(TAG, "GMS installed in virtual space (userId=" + userId + "), returning SUCCESS");
                    return SUCCESS;
                }
                
                // Check if GMS exists on host device
                if (GmsCore.isSupportGms()) {
                    Slog.d(TAG, "GMS available on host device, returning SUCCESS");
                    return SUCCESS;
                }
                
                Slog.d(TAG, "GMS not available, returning SERVICE_MISSING");
                return SERVICE_MISSING;
                
            } catch (Exception e) {
                Slog.w(TAG, "Error checking GMS availability, trying original method", e);
                try {
                    return method.invoke(who, args);
                } catch (Exception e2) {
                    Slog.w(TAG, "Original method also failed, returning SUCCESS to avoid crash", e2);
                    return SUCCESS;
                }
            }
        }
    }

    /**
     * Hook getErrorString to return appropriate messages
     */
    @ProxyMethod("getErrorString")
    public static class GetErrorString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 0 && args[0] instanceof Integer) {
                    int errorCode = (Integer) args[0];
                    if (errorCode == SUCCESS) {
                        return null; // No error
                    }
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error getting error string, returning null", e);
                return null;
            }
        }
    }

    /**
     * Hook isUserRecoverableError to return false for most errors
     */
    @ProxyMethod("isUserRecoverableError")
    public static class IsUserRecoverableError extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 0 && args[0] instanceof Integer) {
                    int errorCode = (Integer) args[0];
                    // If we're returning SUCCESS in availability check, no errors are recoverable
                    if (errorCode == SUCCESS) {
                        return false;
                    }
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error checking if error is recoverable, returning false", e);
                return false;
            }
        }
    }

    /**
     * Hook getErrorResolutionIntent to return null (no resolution needed)
     */
    @ProxyMethod("getErrorResolutionIntent")
    public static class GetErrorResolutionIntent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // If GMS is available in virtual space, no resolution intent needed
                int userId = BlackBoxCore.get().getUserId();
                if (GmsCore.isInstalledGoogleService(userId) || GmsCore.isSupportGms()) {
                    Slog.d(TAG, "GMS available, returning null for resolution intent");
                    return null;
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error getting resolution intent, returning null", e);
                return null;
            }
        }
    }

    /**
     * Hook showErrorNotification to suppress error notifications
     */
    @ProxyMethod("showErrorNotification")
    public static class ShowErrorNotification extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Suppress error notifications if GMS is available
                int userId = BlackBoxCore.get().getUserId();
                if (GmsCore.isInstalledGoogleService(userId) || GmsCore.isSupportGms()) {
                    Slog.d(TAG, "Suppressing GMS error notification");
                    return null;
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error showing notification, suppressing", e);
                return null;
            }
        }
    }

    /**
     * Hook showErrorDialogFragment to suppress error dialogs
     */
    @ProxyMethod("showErrorDialogFragment")
    public static class ShowErrorDialogFragment extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Suppress error dialogs if GMS is available
                int userId = BlackBoxCore.get().getUserId();
                if (GmsCore.isInstalledGoogleService(userId) || GmsCore.isSupportGms()) {
                    Slog.d(TAG, "Suppressing GMS error dialog");
                    return false;
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error showing dialog, suppressing", e);
                return false;
            }
        }
    }

    /**
     * Hook getApkVersion to return a valid version
     */
    @ProxyMethod("getApkVersion")
    public static class GetApkVersion extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Try to get real version from host GMS
                Object result = method.invoke(who, args);
                if (result != null && result instanceof Integer && (Integer) result > 0) {
                    return result;
                }
                // Return a reasonable default version
                Slog.d(TAG, "Returning default GMS version: 23000000");
                return 23000000; // GMS version 23.0.0
            } catch (Exception e) {
                Slog.w(TAG, "Error getting APK version, returning default", e);
                return 23000000;
            }
        }
    }

    /**
     * Hook getClientVersion to return a valid version
     */
    @ProxyMethod("getClientVersion")
    public static class GetClientVersion extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = method.invoke(who, args);
                if (result != null && result instanceof Integer && (Integer) result > 0) {
                    return result;
                }
                return 23000000;
            } catch (Exception e) {
                Slog.w(TAG, "Error getting client version, returning default", e);
                return 23000000;
            }
        }
    }
}
