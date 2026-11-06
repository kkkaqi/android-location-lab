package com.tegufy.blackbox.fake.service;

import java.lang.reflect.Method;

import com.tegufy.blackbox.app.BActivityThread;
import com.tegufy.blackbox.core.system.user.BUserProfileConfig;
import com.tegufy.blackbox.entity.AppConfig;
import com.tegufy.blackbox.fake.hook.ClassInvocationStub;
import com.tegufy.blackbox.fake.hook.MethodHook;
import com.tegufy.blackbox.fake.hook.ProxyMethod;
import com.tegufy.blackbox.utils.Slog;


public class AndroidIdProxy extends ClassInvocationStub {
    public static final String TAG = "AndroidIdProxy";

    public AndroidIdProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    /** Returns the configured spoofAndroidId, or null if not set. */
    private static String getSpoofedAndroidId() {
        AppConfig appConfig = BActivityThread.getAppConfig();
        if (appConfig == null) return null;
        BUserProfileConfig cfg = appConfig.profileConfig;
        if (cfg == null) return null;
        String id = cfg.spoofAndroidId;
        return (id != null && !id.isEmpty()) ? id : null;
    }

    private static boolean isAndroidIdKey(Object[] args) {
        if (args == null || args.length == 0) return false;
        if (!(args[0] instanceof String)) return false;
        String key = (String) args[0];
        return key.contains("android_id") || key.contains("ANDROID_ID")
                || key.contains("secure_id") || key.contains("device_id");
    }

    @ProxyMethod("getAndroidId")
    public static class GetAndroidId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String spoofed = getSpoofedAndroidId();
            if (spoofed != null) return spoofed;
            try {
                Object result = method.invoke(who, args);
                if (result == null || "0".equals(result.toString()) || "".equals(result.toString())) {
                    return generateMockAndroidId();
                }
                return result;
            } catch (Exception e) {
                return generateMockAndroidId();
            }
        }
    }

    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = method.invoke(who, args);
                if (isAndroidIdKey(args)) {
                    String spoofed = getSpoofedAndroidId();
                    if (spoofed != null) return spoofed;
                    if (result == null || "0".equals(result.toString()) || "".equals(result.toString())) {
                        return generateMockAndroidId();
                    }
                }
                return result;
            } catch (Exception e) {
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("getLong")
    public static class GetLong extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = method.invoke(who, args);
                if (isAndroidIdKey(args)) {
                    String spoofed = getSpoofedAndroidId();
                    if (spoofed != null) {
                        try { return Long.parseLong(spoofed, 16); } catch (Exception ignored) {}
                    }
                    if (result == null || ((Number) result).longValue() == 0) {
                        return generateMockAndroidIdLong();
                    }
                }
                return result;
            } catch (Exception e) {
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("get")
    public static class Get extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = method.invoke(who, args);
                if (isAndroidIdKey(args)) {
                    String spoofed = getSpoofedAndroidId();
                    if (spoofed != null) return spoofed;
                    if (result == null || "0".equals(result.toString()) || "".equals(result.toString())) {
                        return generateMockAndroidId();
                    }
                }
                return result;
            } catch (Exception e) {
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("read")
    public static class Read extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = method.invoke(who, args);
                if (isAndroidIdKey(args)) {
                    String spoofed = getSpoofedAndroidId();
                    if (spoofed != null) return spoofed;
                    if (result == null || "0".equals(result.toString()) || "".equals(result.toString())) {
                        return generateMockAndroidId();
                    }
                }
                return result;
            } catch (Exception e) {
                return method.invoke(who, args);
            }
        }
    }

    private static String generateMockAndroidId() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(Integer.toHexString((int) (Math.random() * 16)));
        }
        return sb.toString();
    }

    private static Long generateMockAndroidIdLong() {
        return (long) (Math.random() * Long.MAX_VALUE);
    }
}
