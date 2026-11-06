package com.tegufy.blackbox.fake.service;

import java.lang.reflect.Method;

import black.android.telephony.BRTelephonyManager;
import com.tegufy.blackbox.app.BActivityThread;
import com.tegufy.blackbox.core.system.user.BUserProfileConfig;
import com.tegufy.blackbox.entity.AppConfig;
import com.tegufy.blackbox.fake.hook.ClassInvocationStub;
import com.tegufy.blackbox.fake.hook.MethodHook;
import com.tegufy.blackbox.fake.hook.ProxyMethod;
import com.tegufy.blackbox.utils.MethodParameterUtils;


public class IPhoneSubInfoProxy extends ClassInvocationStub {
    public static final String TAG = "IPhoneSubInfoProxy";

    public IPhoneSubInfoProxy() {
        if (BRTelephonyManager.get()._check_sServiceHandleCacheEnabled() != null) {
            BRTelephonyManager.get()._set_sServiceHandleCacheEnabled(true);
        }
        if (BRTelephonyManager.get()._check_getSubscriberInfoService() != null) {
            BRTelephonyManager.get().getSubscriberInfoService();
        }
    }

    @Override
    protected Object getWho() {
        return BRTelephonyManager.get().sIPhoneSubInfo();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRTelephonyManager.get()._set_sIPhoneSubInfo(proxyInvocation);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstAppPkg(args);
        return super.invoke(proxy, method, args);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }


    private static String spoofedImei() {
        AppConfig cfg = BActivityThread.getAppConfig();
        if (cfg != null && cfg.profileConfig != null) {
            String v = cfg.profileConfig.spoofImei;
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    private static String spoofedImsi() {
        AppConfig cfg = BActivityThread.getAppConfig();
        if (cfg != null && cfg.profileConfig != null) {
            String v = cfg.profileConfig.spoofImsi;
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    @ProxyMethod("getLine1NumberForSubscriber")
    public static class getLine1NumberForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("getImeiForSubscriber")
    public static class GetImeiForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String v = spoofedImei();
            return v != null ? v : method.invoke(who, args);
        }
    }

    @ProxyMethod("getDeviceIdForPhone")
    public static class GetDeviceIdForPhone extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String v = spoofedImei();
            return v != null ? v : method.invoke(who, args);
        }
    }

    @ProxyMethod("getSubscriberIdForSubscriber")
    public static class GetSubscriberIdForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String v = spoofedImsi();
            return v != null ? v : method.invoke(who, args);
        }
    }

    @ProxyMethod("getIccSerialNumberForSubscriber")
    public static class GetIccSerialNumberForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            AppConfig cfg = BActivityThread.getAppConfig();
            if (cfg != null && cfg.profileConfig != null) {
                String v = cfg.profileConfig.spoofIccid;
                if (v != null && !v.isEmpty()) return v;
            }
            return method.invoke(who, args);
        }
    }
}
