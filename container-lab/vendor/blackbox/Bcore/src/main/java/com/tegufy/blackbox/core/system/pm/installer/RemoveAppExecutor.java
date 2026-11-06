package com.tegufy.blackbox.core.system.pm.installer;

import com.tegufy.blackbox.core.env.BEnvironment;
import com.tegufy.blackbox.core.system.pm.BPackageSettings;
import com.tegufy.blackbox.entity.pm.InstallOption;
import com.tegufy.blackbox.utils.FileUtils;


public class RemoveAppExecutor implements Executor {
    @Override
    public int exec(BPackageSettings ps, InstallOption option, int userId) {
        FileUtils.deleteDir(BEnvironment.getAppDir(ps.pkg.packageName));
        return 0;
    }
}
