package com.tegufy.blackbox.core.system.user;

import android.os.Parcel;
import android.os.RemoteException;

import androidx.core.util.AtomicFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.tegufy.blackbox.core.env.BEnvironment;
import com.tegufy.blackbox.core.system.ISystemService;
import com.tegufy.blackbox.core.system.pm.BPackageManagerService;
import com.tegufy.blackbox.utils.CloseUtils;
import com.tegufy.blackbox.utils.FileUtils;
import com.tegufy.blackbox.utils.Slog;


public class BUserManagerService extends IBUserManagerService.Stub implements ISystemService {
    private static final String TAG = "BUserManagerService";
    private static BUserManagerService sService = new BUserManagerService();
    public final HashMap<Integer, BUserInfo> mUsers = new HashMap<>();
    private final HashMap<Integer, BUserProfileConfig> mProfileConfigs = new HashMap<>();
    public final Object mUserLock = new Object();

    public static BUserManagerService get() {
        return sService;
    }

    @Override
    public void systemReady() {
        scanUserL();
    }

    @Override
    public BUserInfo getUserInfo(int userId) {
        synchronized (mUserLock) {
            return mUsers.get(userId);
        }
    }

    @Override
    public boolean exists(int userId) {
        synchronized (mUsers) {
            return mUsers.get(userId) != null;
        }
    }

    @Override
    public BUserInfo createUser(int userId) throws RemoteException {
        synchronized (mUserLock) {
            if (exists(userId)) {
                return getUserInfo(userId);
            }
            return createUserLocked(userId);
        }
    }

    @Override
    public List<BUserInfo> getUsers() {
        synchronized (mUsers) {
            ArrayList<BUserInfo> bUsers = new ArrayList<>();
            for (BUserInfo value : mUsers.values()) {
                if (value.id >= 0) {
                    bUsers.add(value);
                }
            }
            return bUsers;
        }
    }

    public List<BUserInfo> getAllUsers() {
        synchronized (mUsers) {
            return new ArrayList<>(mUsers.values());
        }
    }

    @Override
    public void deleteUser(int userId) throws RemoteException {
        synchronized (mUserLock) {
            synchronized (mUsers) {
                BPackageManagerService.get().deleteUser(userId);

                mUsers.remove(userId);
                saveUserInfoLocked();
                FileUtils.deleteDir(BEnvironment.getUserDir(userId));
                FileUtils.deleteDir(BEnvironment.getExternalUserDir(userId));
            }
        }
    }

    private BUserInfo createUserLocked(int userId) {
        BUserInfo bUserInfo = new BUserInfo();
        bUserInfo.id = userId;
        bUserInfo.status = BUserStatus.ENABLE;
        mUsers.put(userId, bUserInfo);
        synchronized (mUsers) {
            saveUserInfoLocked();
        }
        return bUserInfo;
    }

    private void saveUserInfoLocked() {
        Parcel parcel = Parcel.obtain();
        AtomicFile atomicFile = new AtomicFile(BEnvironment.getUserInfoConf());
        FileOutputStream fileOutputStream = null;
        try {
            ArrayList<BUserInfo> bUsers = new ArrayList<>(mUsers.values());
            parcel.writeTypedList(bUsers);
            try {
                fileOutputStream = atomicFile.startWrite();
                FileUtils.writeParcelToOutput(parcel, fileOutputStream);
                atomicFile.finishWrite(fileOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
                atomicFile.failWrite(fileOutputStream);
            } finally {
                CloseUtils.close(fileOutputStream);
            }
        } finally {
            parcel.recycle();
        }
    }

    private void scanUserL() {
        synchronized (mUserLock) {
            Parcel parcel = Parcel.obtain();
            InputStream is = null;
            try {
                File userInfoConf = BEnvironment.getUserInfoConf();
                if (!userInfoConf.exists()) {
                    return;
                }
                is = new FileInputStream(BEnvironment.getUserInfoConf());
                byte[] bytes = FileUtils.toByteArray(is);
                parcel.unmarshall(bytes, 0, bytes.length);
                parcel.setDataPosition(0);

                ArrayList<BUserInfo> loadUsers = parcel.createTypedArrayList(BUserInfo.CREATOR);
                if (loadUsers == null)
                    return;
                synchronized (mUsers) {
                    mUsers.clear();
                    for (BUserInfo loadUser : loadUsers) {
                        mUsers.put(loadUser.id, loadUser);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                parcel.recycle();
                CloseUtils.close(is);
            }
        }
    }

    @Override
    public BUserProfileConfig getProfileConfig(int userId) {
        synchronized (mProfileConfigs) {
            BUserProfileConfig cached = mProfileConfigs.get(userId);
            if (cached != null) return cached;
        }
        // Load from disk on first access
        BUserProfileConfig config = loadProfileConfig(userId);
        synchronized (mProfileConfigs) {
            mProfileConfigs.put(userId, config);
        }
        return config;
    }

    @Override
    public void setProfileConfig(int userId, BUserProfileConfig config) {
        if (config == null) config = new BUserProfileConfig();
        synchronized (mProfileConfigs) {
            mProfileConfigs.put(userId, config);
        }
        saveProfileConfig(userId, config);
    }

    private BUserProfileConfig loadProfileConfig(int userId) {
        File confFile = BEnvironment.getProfileConf(userId);
        if (!confFile.exists()) return new BUserProfileConfig();
        Parcel parcel = Parcel.obtain();
        InputStream is = null;
        try {
            is = new FileInputStream(confFile);
            byte[] bytes = FileUtils.toByteArray(is);
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            BUserProfileConfig config = BUserProfileConfig.CREATOR.createFromParcel(parcel);
            return config != null ? config : new BUserProfileConfig();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to load profile config for user " + userId, e);
            return new BUserProfileConfig();
        } finally {
            parcel.recycle();
            CloseUtils.close(is);
        }
    }

    private void saveProfileConfig(int userId, BUserProfileConfig config) {
        File confFile = BEnvironment.getProfileConf(userId);
        Slog.d(TAG, "saveProfileConfig userId=" + userId + " path=" + confFile.getAbsolutePath() + " config=" + config);
        Parcel parcel = Parcel.obtain();
        AtomicFile atomicFile = new AtomicFile(confFile);
        FileOutputStream fos = null;
        try {
            config.writeToParcel(parcel, 0);
            fos = atomicFile.startWrite();
            FileUtils.writeParcelToOutput(parcel, fos);
            atomicFile.finishWrite(fos);
            Slog.d(TAG, "saveProfileConfig OK, file size=" + confFile.length());
        } catch (IOException e) {
            Slog.e(TAG, "Failed to save profile config for user " + userId, e);
            atomicFile.failWrite(fos);
        } finally {
            parcel.recycle();
            CloseUtils.close(fos);
        }
    }
}