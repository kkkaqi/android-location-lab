package com.tegufy.blackbox.core.system;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tegufy.blackbox.BlackBoxCore;
import com.tegufy.blackbox.core.env.AppSystemEnv;
import com.tegufy.blackbox.core.env.BEnvironment;
import com.tegufy.blackbox.core.system.accounts.BAccountManagerService;
import com.tegufy.blackbox.core.system.am.BActivityManagerService;
import com.tegufy.blackbox.core.system.am.BJobManagerService;
import com.tegufy.blackbox.core.system.location.BLocationManagerService;
import com.tegufy.blackbox.core.system.notification.BNotificationManagerService;
import com.tegufy.blackbox.core.system.os.BStorageManagerService;
import com.tegufy.blackbox.core.system.pm.BPackageInstallerService;
import com.tegufy.blackbox.core.system.pm.BPackageManagerService;

import com.tegufy.blackbox.core.system.user.BUserHandle;
import com.tegufy.blackbox.core.system.user.BUserManagerService;
import com.tegufy.blackbox.entity.pm.InstallOption;
import com.tegufy.blackbox.utils.FileUtils;

import com.tegufy.blackbox.core.system.JarManager;


public class BlackBoxSystem {
    private static BlackBoxSystem sBlackBoxSystem;
    private final List<ISystemService> mServices = new ArrayList<>();
    private final static AtomicBoolean isStartup = new AtomicBoolean(false);

    public static BlackBoxSystem getSystem() {
        if (sBlackBoxSystem == null) {
            synchronized (BlackBoxSystem.class) {
                if (sBlackBoxSystem == null) {
                    sBlackBoxSystem = new BlackBoxSystem();
                }
            }
        }
        return sBlackBoxSystem;
    }

    public void startup() {
        if (isStartup.getAndSet(true))
            return;
        BEnvironment.load();

        mServices.add(BPackageManagerService.get());
        mServices.add(BUserManagerService.get());
        mServices.add(BActivityManagerService.get());
        mServices.add(BJobManagerService.get());
        mServices.add(BStorageManagerService.get());
        mServices.add(BPackageInstallerService.get());

        mServices.add(BProcessManagerService.get());
        mServices.add(BAccountManagerService.get());
        mServices.add(BLocationManagerService.get());
        mServices.add(BNotificationManagerService.get());

        for (ISystemService service : mServices) {
            service.systemReady();
        }

        List<String> preInstallPackages = AppSystemEnv.getPreInstallPackages();
        for (String preInstallPackage : preInstallPackages) {
            try {
                if (!BPackageManagerService.get().isInstalled(preInstallPackage, BUserHandle.USER_ALL)) {
                    PackageInfo packageInfo = BlackBoxCore.getPackageManager().getPackageInfo(preInstallPackage, 0);
                    BPackageManagerService.get().installPackageAsUser(packageInfo.applicationInfo.sourceDir, InstallOption.installBySystem(), BUserHandle.USER_ALL);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        
        JarManager.getInstance().initializeAsync();
        
        
     
    }
}
