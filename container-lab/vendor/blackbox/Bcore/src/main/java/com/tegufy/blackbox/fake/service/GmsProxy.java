package com.tegufy.blackbox.fake.service;

import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import com.tegufy.blackbox.BlackBoxCore;
import com.tegufy.blackbox.fake.hook.BinderInvocationStub;
import com.tegufy.blackbox.fake.hook.MethodHook;
import com.tegufy.blackbox.fake.hook.ProxyMethod;
import com.tegufy.blackbox.utils.Slog;


public class GmsProxy extends BinderInvocationStub {
    public static final String TAG = "GmsProxy";

    public GmsProxy() {
        super(getGmsBinder());
    }

    private static IBinder getGmsBinder() {
        try {
            return BRServiceManager.get().getService("gms");
        } catch (Throwable e) {
            Slog.d(TAG, "GMS service binder not available in ServiceManager");
            return null;
        }
    }

    @Override
    protected Object getWho() {
        try {
            IBinder binder = BRServiceManager.get().getService("gms");
            if (binder == null) {
                Slog.d(TAG, "GMS service binder not available, skipping proxy initialization");
                return null;
            }
            Class<?> stubClass = Class.forName("com.google.android.gms.common.api.internal.IGmsServiceBroker$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            Object iface = asInterfaceMethod.invoke(null, binder);
            if (iface != null) {
                Slog.d(TAG, "Successfully obtained IGmsServiceBroker interface");
                return iface;
            } else {
                Slog.d(TAG, "IGmsServiceBroker interface is null");
                return null;
            }
        } catch (Throwable e) {
            Slog.d(TAG, "GMS classes not available: " + e.getClass().getSimpleName());
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        try {
            replaceSystemService("gms");
            Slog.d(TAG, "GMS proxy injected successfully");
        } catch (Throwable e) {
            Slog.w(TAG, "Failed to inject GMS proxy", e);
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("getService")
    public static class GetService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    String callingPackage = (String) args[0];
                    if ("com.google.android.gms".equals(callingPackage)) {
                        
                        args[0] = BlackBoxCore.getHostPkg();
                        Slog.d(TAG, "GmsProxy: Replaced calling package to " + BlackBoxCore.getHostPkg());
                    }
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.e(TAG, "GmsProxy: Error in getService", e);
                
                return null;
            }
        }
    }

    
    @ProxyMethod("getServiceBroker")
    public static class GetServiceBroker extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.e(TAG, "GmsProxy: Error in getServiceBroker", e);
                
                return null;
            }
        }
    }

    
    @ProxyMethod("authenticate")
    public static class Authenticate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling authenticate call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: Authentication error, returning mock result", e);
                
                return createMockAuthResult();
            }
        }
    }

    
    @ProxyMethod("getAccount")
    public static class GetAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getAccount call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetAccount error, returning null", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("getToken")
    public static class GetToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetToken error, returning mock token", e);
                return "mock_gms_token_" + System.currentTimeMillis();
            }
        }
    }

    
    @ProxyMethod("invalidateToken")
    public static class InvalidateToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling invalidateToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: InvalidateToken error, ignoring", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("clearToken")
    public static class ClearToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling clearToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: ClearToken error, ignoring", e);
                return null;
            }
        }
    }

    
    private static Object createMockAuthResult() {
        try {
            
            Class<?> bundleClass = Class.forName("android.os.Bundle");
            return bundleClass.newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock auth result", e);
            return null;
        }
    }
}
