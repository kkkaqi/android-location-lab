package app.containerlab;

final class RendererCompatPolicy {
    static boolean applies(boolean bound, int user, String packageName, String processName,
                           int sdk, String manufacturer) {
        if (!bound || user != 0 || sdk != 35 || !"com.tencent.mm".equals(packageName)
                || !("OnePlus".equalsIgnoreCase(manufacturer) || "OPPO".equalsIgnoreCase(manufacturer))
                || processName == null) return false;
        String prefix = "com.tencent.mm:xweb_sandboxed_process_";
        if (!processName.startsWith(prefix) || processName.length() == prefix.length()) return false;
        for (int i = prefix.length(); i < processName.length(); i++)
            if (processName.charAt(i) < '0' || processName.charAt(i) > '9') return false;
        return true;
    }
}
