package app.containerlab;

import com.tegufy.blackbox.utils.compat.HostPermissionPolicy;

public final class HostPermissionPolicyTest {
    private static int checks;
    private static final String INTERNET = "android.permission.INTERNET";
    private static void check(boolean value, String reason) {
        checks++; if (!value) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        Object[] call = { INTERNET, 2345, 10001, 77 };
        Object[] mapped = HostPermissionPolicy.activity(call, 10001, 10123, 2345, true);
        check(mapped != null && mapped[2].equals(10123), "Android15 device permission query maps UID to host");
        check(mapped[3].equals(77), "device ID is preserved");
        check(call[2].equals(10001), "original arguments are not mutated");
        check(HostPermissionPolicy.activity(call, 10001, 10123, 2345, false) == null, "no active guest config cannot bridge");
        check(HostPermissionPolicy.activity(call, 10001, 10123, 9876, true) == null, "another PID cannot bridge");
        check(HostPermissionPolicy.activity(new Object[]{INTERNET,2345,99999,0}, 10001,10123,2345,true) == null, "another UID cannot bridge");
        check(HostPermissionPolicy.activity(new Object[]{INTERNET,2345,10123}, 10001,10123,2345,true)[2].equals(10123), "own actual UID remains actual UID");
        check(HostPermissionPolicy.activity(new Object[]{INTERNET,"bad",10001},10001,10123,2345,true) == null, "malformed query delegates unchanged");
        for (String permission : new String[]{"android.permission.SEND_SMS", "android.permission.RECORD_AUDIO", "android.permission.CHANGE_WIFI_STATE"}) {
            check(HostPermissionPolicy.activity(new Object[]{permission,2345,10001},10001,10123,2345,true) == null, "sensitive/unrelated permission is not added: " + permission);
        }
        Object[] pkg = HostPermissionPolicy.packageManager(new Object[]{INTERNET,"com.tencent.mm",0}, "com.tencent.mm",0,"app.containerlab",0);
        check(pkg != null && "app.containerlab".equals(pkg[1]), "package query maps own guest to host");
        check(HostPermissionPolicy.packageManager(new Object[]{INTERNET,"other.app",0},"com.tencent.mm",0,"app.containerlab",0) == null, "another package unchanged");
        check(HostPermissionPolicy.packageManager(new Object[]{INTERNET,"com.tencent.mm",1},"com.tencent.mm",0,"app.containerlab",0) == null, "another virtual user unchanged");
        check(HostPermissionPolicy.packageManager(new Object[]{INTERNET,"com.tencent.mm",0},null,0,"app.containerlab",0) == null, "host/server process has no guest query mapping");
        Object[] modern = HostPermissionPolicy.permissionManager(new Object[]{"com.tencent.mm",INTERNET,"default:0",0},"com.tencent.mm",0,"app.containerlab",0);
        check(modern != null && "app.containerlab".equals(modern[0]) && "default:0".equals(modern[2]), "modern package-first API preserves persistent device ID");
        Object[] uid = HostPermissionPolicy.uidPermission(new Object[]{10001,INTERNET,91},true,10001,10123,true);
        check(uid != null && uid[0].equals(10123) && uid[2].equals(91), "modern uid-first API maps only UID");
        check(HostPermissionPolicy.uidPermission(new Object[]{INTERNET,10001},false,10001,10123,true)[1].equals(10123), "legacy permission-first UID API supported");
        check(HostPermissionPolicy.grantFlag(INTERNET,0,true,2) == 2, "metadata reflects actual host grant");
        check(HostPermissionPolicy.grantFlag(INTERNET,3,false,2) == 1, "actual host denial clears only grant bit");
        check(HostPermissionPolicy.grantFlag("android.permission.SEND_SMS",1,true,2) == 1, "metadata leaves unrelated permissions untouched");
        check(HostPermissionPolicy.isGuest("com.tencent.mm",0) && !HostPermissionPolicy.isGuest("com.tencent.mm",1), "prototype scope is WeChat user0 only");
        for (String permission : new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.CAMERA"}) {
            Object[] location = HostPermissionPolicy.activity(new Object[]{permission,2345,10001,0},10001,10123,2345,true);
            check(location != null && location[0].equals(permission) && location[2].equals(10123), "resource permission query must reach actual host UID: " + permission);
            Object[] locationPkg = HostPermissionPolicy.packageManager(new Object[]{permission,"com.tencent.mm",0},"com.tencent.mm",0,"app.containerlab",0);
            check(locationPkg != null && locationPkg[0].equals(permission) && locationPkg[1].equals("app.containerlab"), "location package query uses actual host identity");
            check(HostPermissionPolicy.activity(new Object[]{permission,2345,99999},10001,10123,2345,true) == null, "location query cannot bridge other UIDs");
            Object[] modernLocation = HostPermissionPolicy.permissionManager(new Object[]{"com.tencent.mm",permission,"device:7",0},"com.tencent.mm",0,"app.containerlab",2);
            check(modernLocation != null && modernLocation[0].equals("app.containerlab") && modernLocation[2].equals("device:7") && modernLocation[3].equals(2), "modern location query preserves device ID and maps system user");
            check(HostPermissionPolicy.uidPermission(new Object[]{10001,permission,7},true,10001,10123,true)[0].equals(10123), "modern location UID query reaches host");
            check(HostPermissionPolicy.packageManager(new Object[]{permission,"other.app",0},"com.tencent.mm",0,"app.containerlab",0) == null, "another package's location query remains unchanged");
            check(HostPermissionPolicy.packageManager(new Object[]{permission,"com.tencent.mm",1},"com.tencent.mm",1,"app.containerlab",0) == null, "another virtual user's location query remains unchanged");
            check(HostPermissionPolicy.grantFlag(permission,3,false,2) == 1, "actual location denial clears only the granted flag");
        }
        System.out.println("PASS: " + checks + " host permission query checks");
    }
}
