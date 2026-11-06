package com.tegufy.blackbox.core.system.pm.installer;

import com.tegufy.blackbox.core.system.pm.BPackageSettings;
import com.tegufy.blackbox.entity.pm.InstallOption;


public interface Executor {
    public static final String TAG = "InstallExecutor";

    int exec(BPackageSettings ps, InstallOption option, int userId);
}
