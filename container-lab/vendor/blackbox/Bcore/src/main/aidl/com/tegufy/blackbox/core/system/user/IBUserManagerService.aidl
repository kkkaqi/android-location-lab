// IBUserManagerService.aidl
package com.tegufy.blackbox.core.system.user;

// Declare any non-default types here with import statements
import com.tegufy.blackbox.core.system.user.BUserInfo;
import com.tegufy.blackbox.core.system.user.BUserProfileConfig;
import java.util.List;


interface IBUserManagerService {
    BUserInfo getUserInfo(int userId);
    boolean exists(int userId);
    BUserInfo createUser(int userId);
    List<BUserInfo> getUsers();
    void deleteUser(int userId);

    // Per-profile device spoof + xposed config
    BUserProfileConfig getProfileConfig(int userId);
    void setProfileConfig(int userId, in BUserProfileConfig config);
}
