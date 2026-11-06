package com.tegufy.blackbox.core.system;

import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.ConditionVariable;
import android.os.IInterface;
import android.os.Process;

import java.util.Arrays;

import com.tegufy.blackbox.core.IBActivityThread;
import com.tegufy.blackbox.core.env.BEnvironment;
import com.tegufy.blackbox.core.system.user.BUserProfileConfig;
import com.tegufy.blackbox.entity.AppConfig;
import com.tegufy.blackbox.proxy.ProxyManifest;
import com.tegufy.blackbox.utils.CloseUtils;
import com.tegufy.blackbox.utils.FileUtils;

public class ProcessRecord extends Binder {
    public final ApplicationInfo info;
    final public String processName;
    public IBActivityThread bActivityThread;
    public IInterface appThread;
    public int uid;
    public int pid;
    public int buid;
    public int bpid;
    public int callingBUid;
    public int userId;

    public ConditionVariable initLock = new ConditionVariable();

    public ProcessRecord(ApplicationInfo info, String processName) {
        this.info = info;
        this.processName = processName;
    }

    public int getCallingBUid() {
        return callingBUid;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{processName, pid, buid, bpid, uid, pid, userId});
    }

    public String getProviderAuthority() {
        return ProxyManifest.getProxyAuthorities(bpid);
    }

    public AppConfig getClientConfig() {
        AppConfig config = new AppConfig();
        config.packageName = info.packageName;
        config.processName = processName;
        config.bpid = bpid;
        config.buid = buid;
        config.uid = uid;
        config.callingBUid = callingBUid;
        config.userId = userId;
        config.token = this;
        config.profileConfig = loadProfileConfig(userId);
        return config;
    }

    private static BUserProfileConfig loadProfileConfig(int userId) {
        java.io.File confFile = BEnvironment.getProfileConf(userId);
        android.util.Log.d("ProcessRecord", "loadProfileConfig userId=" + userId + " path=" + confFile.getAbsolutePath() + " exists=" + confFile.exists());
        if (!confFile.exists()) return null;
        android.os.Parcel parcel = android.os.Parcel.obtain();
        java.io.InputStream is = null;
        try {
            is = new java.io.FileInputStream(confFile);
            byte[] bytes = FileUtils.toByteArray(is);
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            BUserProfileConfig cfg = BUserProfileConfig.CREATOR.createFromParcel(parcel);
            android.util.Log.d("ProcessRecord", "loadProfileConfig result=" + cfg);
            return cfg;
        } catch (Exception e) {
            android.util.Log.e("ProcessRecord", "loadProfileConfig failed", e);
            return null;
        } finally {
            parcel.recycle();
            CloseUtils.close(is);
        }
    }

    public void kill() {
        if (pid > 0) {
            try {
                Process.killProcess(pid);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public String getPackageName() {
        return info.packageName;
    }
}
