package com.tegufy.blackbox.fake.service;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.util.Log;

import java.lang.reflect.Method;

import black.android.net.wifi.BRIWifiManagerStub;
import black.android.net.wifi.BRWifiInfo;
import black.android.net.wifi.BRWifiSsid;
import black.android.os.BRServiceManager;
import com.tegufy.blackbox.app.BActivityThread;
import com.tegufy.blackbox.core.system.user.BUserProfileConfig;
import com.tegufy.blackbox.entity.AppConfig;
import com.tegufy.blackbox.fake.hook.BinderInvocationStub;
import com.tegufy.blackbox.fake.hook.MethodHook;
import com.tegufy.blackbox.fake.hook.ProxyMethod;


public class IWifiManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IWifiManagerProxy";

    public IWifiManagerProxy() {
        super(BRServiceManager.get().getService(Context.WIFI_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIWifiManagerStub.get().asInterface(BRServiceManager.get().getService(Context.WIFI_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    private static String getSpoofedMac() {
        AppConfig cfg = BActivityThread.getAppConfig();
        if (cfg == null) return null;
        BUserProfileConfig p = cfg.profileConfig;
        if (p != null && p.spoofMac != null && !p.spoofMac.isEmpty()) return p.spoofMac;
        return null;
    }

    private static final String DEFAULT_MAC  = "ac:62:5a:82:65:c4";
    private static final String DEFAULT_SSID = "BlackBox_Wifi";

    @ProxyMethod("getConnectionInfo")
    public static class GetConnectionInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            WifiInfo wifiInfo = (WifiInfo) method.invoke(who, args);
            String mac = getSpoofedMac();
            if (mac == null) mac = DEFAULT_MAC;
            BRWifiInfo.get(wifiInfo)._set_mBSSID(mac);
            BRWifiInfo.get(wifiInfo)._set_mMacAddress(mac);
            BRWifiInfo.get(wifiInfo)._set_mWifiSsid(BRWifiSsid.get().createFromAsciiEncoded(DEFAULT_SSID));
            return wifiInfo;
        }

        public static String intIP2StringIP(int ip) {
            return (ip & 0xFF) + "." +
                    ((ip >> 8) & 0xFF) + "." +
                    ((ip >> 16) & 0xFF) + "." +
                    (ip >> 24 & 0xFF);
        }

        public static int ip2Int(String ipString) {
            
            String[] ipSlices = ipString.split("\\.");
            int rs = 0;
            for (int i = 0; i < ipSlices.length; i++) {
                
                int intSlice = Integer.parseInt(ipSlices[i]) << 8 * i;
                
                rs = rs | intSlice;
            }
            return rs;
        }
    }
}
