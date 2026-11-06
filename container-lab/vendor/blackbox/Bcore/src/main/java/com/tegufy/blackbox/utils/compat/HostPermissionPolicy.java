package com.tegufy.blackbox.utils.compat;

/** Own-guest network/location/camera queries use the system's actual host grant or denial. */
public final class HostPermissionPolicy {
    private HostPermissionPolicy() {}
    public static boolean isNetwork(String permission) {
        return "android.permission.INTERNET".equals(permission) || "android.permission.ACCESS_NETWORK_STATE".equals(permission);
    }
    public static boolean supported(String permission) {
        return isNetwork(permission) || "android.permission.ACCESS_FINE_LOCATION".equals(permission)
                || "android.permission.ACCESS_COARSE_LOCATION".equals(permission)
                || "android.permission.ACCESS_BACKGROUND_LOCATION".equals(permission)
                || "android.permission.CAMERA".equals(permission);
    }
    public static boolean isGuest(String pkg, int user) { return "com.tencent.mm".equals(pkg) && user == 0; }
    private static boolean ownUid(Object uid, int guestUid, int hostUid) {
        return guestUid > 0 && hostUid > 0 && uid instanceof Integer
                && (((Integer) uid) == guestUid || ((Integer) uid) == hostUid);
    }
    public static Object[] activity(Object[] args, int guestUid, int hostUid, int pid, boolean active) {
        if (!active || args == null || (args.length != 3 && args.length != 4)
                || !(args[0] instanceof String) || !supported((String) args[0])
                || !(args[1] instanceof Integer) || ((Integer) args[1]) != pid
                || !ownUid(args[2], guestUid, hostUid)
                || (args.length == 4 && !(args[3] instanceof Integer))) return null;
        Object[] result = args.clone(); result[2] = hostUid; return result;
    }
    public static Object[] packageManager(Object[] args, String guestPkg, int guestUser, String hostPkg, int hostUser) {
        if (!isGuest(guestPkg, guestUser) || args == null || (args.length != 2 && args.length != 3)
                || !(args[0] instanceof String) || !supported((String) args[0])
                || !guestPkg.equals(args[1]) || hostPkg == null
                || (args.length == 3 && !Integer.valueOf(guestUser).equals(args[2]))) return null;
        Object[] result = args.clone(); result[1] = hostPkg;
        if (result.length == 3) result[2] = hostUser;
        return result;
    }
    public static Object[] permissionManager(Object[] args, String guestPkg, int guestUser, String hostPkg, int hostUser) {
        if (!isGuest(guestPkg, guestUser) || args == null || (args.length != 3 && args.length != 4)
                || !guestPkg.equals(args[0]) || !(args[1] instanceof String) || !supported((String) args[1])
                || !Integer.valueOf(guestUser).equals(args[args.length - 1]) || hostPkg == null
                || (args.length == 4 && args[2] != null && !(args[2] instanceof String))) return null;
        Object[] result = args.clone(); result[0] = hostPkg; result[result.length - 1] = hostUser; return result;
    }
    public static Object[] uidPermission(Object[] args, boolean uidFirst, int guestUid, int hostUid, boolean active) {
        int uidIndex = uidFirst ? 0 : 1;
        int permIndex = uidFirst ? 1 : 0;
        if (!active || args == null || args.length != (uidFirst ? 3 : 2)
                || !(args[permIndex] instanceof String) || !supported((String) args[permIndex])
                || !ownUid(args[uidIndex], guestUid, hostUid)
                || (uidFirst && !(args[2] instanceof Integer))) return null;
        Object[] result = args.clone(); result[uidIndex] = hostUid; return result;
    }
    public static int grantFlag(String permission, int oldFlags, boolean actualHostGrant, int grantedBit) {
        if (!supported(permission)) return oldFlags;
        return actualHostGrant ? oldFlags | grantedBit : oldFlags & ~grantedBit;
    }
}
