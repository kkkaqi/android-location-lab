package app.containerlab;

import java.util.Arrays;

public final class ImportPolicy {
    public static final String WECHAT = "com.tencent.mm";
    private ImportPolicy() {}
    public static boolean importConfirmed(boolean reportedSuccess, String reportedPackage, boolean installed) {
        return reportedSuccess && WECHAT.equals(reportedPackage) && installed;
    }
    public static String architectureError(String[] hostAbis, String[] guestAbis) {
        if (!Arrays.asList(hostAbis).contains("arm64-v8a")) return "测试版只支持 ARM64 手机。";
        if (guestAbis.length == 0) return "无法确认微信原生库架构，请复制诊断。";
        if (!Arrays.asList(guestAbis).contains("arm64-v8a")) return "本机微信没有 ARM64 原生库，无法用这个64位容器测试。";
        return null;
    }
}
