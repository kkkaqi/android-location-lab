// IBPackageInstallerService.aidl
package com.tegufy.blackbox.core.system.pm;

import com.tegufy.blackbox.core.system.pm.BPackageSettings;
import com.tegufy.blackbox.entity.pm.InstallOption;

// Declare any non-default types here with import statements

interface IBPackageInstallerService {
    int installPackageAsUser(in BPackageSettings ps, int userId);
    int uninstallPackageAsUser(in BPackageSettings ps, boolean removeApp, int userId);
    int clearPackage(in BPackageSettings ps, int userId);
    int updatePackage(in BPackageSettings ps);
}
