package com.tegufy.blackbox.utils.compat;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import com.tegufy.blackbox.BlackBoxCore;
import com.tegufy.blackbox.app.BActivityThread;

/** A mapped call invokes the original binder to obtain the actual host grant. */
public final class HostPermissions {
    private HostPermissions() {}
    private static boolean active() {
        return BActivityThread.getAppConfig() != null && HostPermissionPolicy.isGuest(BActivityThread.getAppPackageName(), BActivityThread.getUserId());
    }
    private static int hostUser() { return BlackBoxCore.getHostUid() / 100000; }
    public static Object[] activity(Object[] args) {
        return HostPermissionPolicy.activity(args, BActivityThread.getBUid(), BlackBoxCore.getHostUid(), Process.myPid(), active());
    }
    public static Object[] packageManager(Object[] args) {
        if (!active()) return null;
        return HostPermissionPolicy.packageManager(args, BActivityThread.getAppPackageName(), BActivityThread.getUserId(), BlackBoxCore.getHostPkg(), hostUser());
    }
    public static Object[] permissionManager(Object[] args) {
        if (!active()) return null;
        return HostPermissionPolicy.permissionManager(args, BActivityThread.getAppPackageName(), BActivityThread.getUserId(), BlackBoxCore.getHostPkg(), hostUser());
    }
    public static Object[] uidPermission(Object[] args, boolean uidFirst) {
        return HostPermissionPolicy.uidPermission(args, uidFirst, BActivityThread.getBUid(), BlackBoxCore.getHostUid(), active());
    }
    public static void mirrorFlags(PackageInfo info, int guestUser) {
        if (info == null || !HostPermissionPolicy.isGuest(info.packageName, guestUser) || info.requestedPermissions == null) return;
        if (info.requestedPermissionsFlags == null) info.requestedPermissionsFlags = new int[info.requestedPermissions.length];
        else if (info.requestedPermissionsFlags.length != info.requestedPermissions.length) info.requestedPermissionsFlags = java.util.Arrays.copyOf(info.requestedPermissionsFlags, info.requestedPermissions.length);
        for (int i = 0; i < info.requestedPermissions.length; i++) {
            String permission = info.requestedPermissions[i];
            if (!HostPermissionPolicy.supported(permission)) continue;
            // Host package differs from the guest, so permission hooks delegate unchanged.
            boolean granted = BlackBoxCore.getPackageManager().checkPermission(permission, BlackBoxCore.getHostPkg()) == PackageManager.PERMISSION_GRANTED;
            info.requestedPermissionsFlags[i] = HostPermissionPolicy.grantFlag(permission, info.requestedPermissionsFlags[i], granted, PackageInfo.REQUESTED_PERMISSION_GRANTED);
        }
    }
}
