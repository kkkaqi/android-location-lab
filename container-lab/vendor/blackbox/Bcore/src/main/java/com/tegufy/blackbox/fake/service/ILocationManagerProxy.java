package com.tegufy.blackbox.fake.service;

import android.content.Context;
import android.location.LocationManager;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import black.android.location.BRILocationListener;
import black.android.location.BRILocationManagerStub;
import black.android.location.provider.BRProviderProperties;
import black.android.location.provider.ProviderProperties;
import black.android.os.BRServiceManager;
import com.tegufy.blackbox.app.BActivityThread;
import com.tegufy.blackbox.entity.location.BLocation;
import com.tegufy.blackbox.fake.frameworks.BLocationManager;
import com.tegufy.blackbox.fake.hook.BinderInvocationStub;
import com.tegufy.blackbox.fake.hook.MethodHook;
import com.tegufy.blackbox.fake.hook.ProxyMethod;
import com.tegufy.blackbox.utils.MethodParameterUtils;


public class ILocationManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ILocationManagerProxy";
    private static Object installedProxy;
    private final Object contextDelegate;

    public ILocationManagerProxy() {
        super(BRServiceManager.get().getService(Context.LOCATION_SERVICE));
        contextDelegate = null;
    }

    private ILocationManagerProxy(IInterface delegate) {
        super(delegate.asBinder());
        contextDelegate = delegate;
    }

    @Override
    protected Object getWho() {
        if (contextDelegate != null) return contextDelegate;
        try {
            java.lang.reflect.Method factory = Class.forName("android.location.ILocationManager$Stub").getDeclaredMethod("asInterface", android.os.IBinder.class);
            factory.setAccessible(true);
            return factory.invoke(null, BRServiceManager.get().getService(Context.LOCATION_SERVICE));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Location service interface unavailable", error);
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        installedProxy = proxyInvocation;
        replaceSystemService(Context.LOCATION_SERVICE);
    }

    /** Guest initialization may replace the framework service. Wrap its current delegate,
     * preserving its behavior for requests that our scoped adapter does not handle. */
    public static void fixContext(Context context) {
        if (context == null || BActivityThread.getAppConfig() == null
                || BActivityThread.getUserId() != 0
                || !"com.tencent.mm".equals(BActivityThread.getAppPackageName())) return;
        try {
            Object manager = context.getSystemService(Context.LOCATION_SERVICE);
            if (manager != null) {
                java.lang.reflect.Field service = LocationManager.class.getDeclaredField("mService");
                service.setAccessible(true);
                Object current = service.get(manager);
                if (current == null) throw new IllegalStateException("Location service unavailable");
                if (java.lang.reflect.Proxy.isProxyClass(current.getClass())
                        && java.lang.reflect.Proxy.getInvocationHandler(current) instanceof ILocationManagerProxy) return;
                ILocationManagerProxy adapter = new ILocationManagerProxy((IInterface) current);
                adapter.injectHook();
                service.set(manager, adapter.getProxyInvocation());
            }
        } catch (Exception error) {
            Log.w(TAG, "Unable to connect cached LocationManager to guest service", error);
        }
    }
    public static String diagnostic(Context context) {
        try {
            java.lang.reflect.Field field = LocationManager.class.getDeclaredField("mService");field.setAccessible(true);
            Object service = field.get(context.getSystemService(Context.LOCATION_SERVICE));
            String handler = service != null && java.lang.reflect.Proxy.isProxyClass(service.getClass()) ? java.lang.reflect.Proxy.getInvocationHandler(service).getClass().getName() : "none";
            return "installed=" + (installedProxy != null) + " handler=" + handler;
        } catch (Exception error) { return "unavailable="+error.getClass().getSimpleName(); }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object scoped = com.tegufy.blackbox.utils.compat.ScopedLocation.intercept(method, args);
        if (scoped != com.tegufy.blackbox.utils.compat.ScopedLocation.UNHANDLED) return scoped;
        MethodParameterUtils.replaceFirstAppPkg(args);
        
        
        String packageName = BActivityThread.getAppPackageName();
        if (packageName != null && packageName.equals("com.google.android.gms")) {
            
            if (method.getName().equals("getLastLocation") || 
                method.getName().equals("getLastKnownLocation") ||
                method.getName().equals("requestLocationUpdates")) {
                Log.w(TAG, "Blocking location request from Google Play Services to prevent crash");
                return null;
            }
        }
        
        if (!com.tegufy.blackbox.utils.compat.ScopedLocation.isActive()) {
            try { return method.invoke(getBase(), args); }
            catch (java.lang.reflect.InvocationTargetException error) { throw error.getCause(); }
        }
        return super.invoke(proxy, method, args);
    }

    @ProxyMethod("registerGnssStatusCallback")
    public static class RegisterGnssStatusCallback extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            return true;
        }
    }

    @ProxyMethod("getLastLocation")
    public static class GetLastLocation extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                return BLocationManager.get().getLocation(BActivityThread.getUserId(), BActivityThread.getAppPackageName()).convert2SystemLocation();
            }
            
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Log.w(TAG, "Location permission denied, returning null for getLastLocation");
                    return null;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getLastKnownLocation")
    public static class GetLastKnownLocation extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                return BLocationManager.get().getLocation(BActivityThread.getUserId(), BActivityThread.getAppPackageName()).convert2SystemLocation();
            }
            
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Log.w(TAG, "Location permission denied, returning null for getLastKnownLocation");
                    return null;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("requestLocationUpdates")
    public static class RequestLocationUpdates extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                if (args[1] instanceof IInterface) {
                    IInterface listener = (IInterface) args[1];
                    BLocationManager.get().requestLocationUpdates(listener.asBinder());
                    return 0;
                }
            }
            
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Log.w(TAG, "Location permission denied for requestLocationUpdates, returning 0");
                    return 0;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("removeUpdates")
    public static class RemoveUpdates extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args[0] instanceof IInterface) {
                IInterface listener = (IInterface) args[0];
                BLocationManager.get().removeUpdates(listener.asBinder());
                return 0;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getProviderProperties")
    public static class GetProviderProperties extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object providerProperties = method.invoke(who, args);
            if (BLocationManager.isFakeLocationEnable() && providerProperties != null) {
                BRProviderProperties.get(providerProperties)._set_mHasNetworkRequirement(false);
                if (BLocationManager.get().getCell(BActivityThread.getUserId(), BActivityThread.getAppPackageName()) == null) {
                    BRProviderProperties.get(providerProperties)._set_mHasCellRequirement(false);
                }
            }
            return providerProperties;
        }
    }

    @ProxyMethod("removeGpsStatusListener")
    public static class RemoveGpsStatusListener extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            return 0;
        }
    }

    @ProxyMethod("getBestProvider")
    public static class GetBestProvider extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                return LocationManager.GPS_PROVIDER;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getAllProviders")
    public static class GetAllProviders extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) return Arrays.asList(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("isProviderEnabledForUser")
    public static class isProviderEnabledForUser extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (!BLocationManager.isFakeLocationEnable()) return method.invoke(who, args);
            String provider = (String) args[0];
            return Objects.equals(provider, LocationManager.GPS_PROVIDER);
        }
    }

    @ProxyMethod("setExtraLocationControllerPackageEnabled")
    public static class setExtraLocationControllerPackageEnabled extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }
}
