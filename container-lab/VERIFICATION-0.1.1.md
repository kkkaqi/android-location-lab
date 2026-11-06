# 0.1.1 联网页面兼容补丁

用户的 OnePlus PKG110 / Android15 / ColorOS15、微信8.0.78已进入容器内真实微信 Application 和登录流程。请求验证码时，官方登录安全检查网页显示 ERR_CACHE_MISS。没有访问用户含 ticket 的网址、使用其凭据或改变安全检查响应。

## 诊断依据

- 上游 NewBlackbox issue68 记录同类“Java请求可用，但WebView全部ERR_CACHE_MISS”的权限映射问题。
- Chromium AwSettings 构造时调用 Context.checkSelfPermission(INTERNET) 并据此初始化 blockNetworkLoads。
- Android15 PermissionManager 的同进程检查走 AMS.checkPermissionForDevice(permission,pid,uid,deviceId)。当前分支缺少该钩子；不能用替换最后一个整数的方法，因为最后一个整数是deviceId。
- 当前分支的虚拟包权限元数据没有给正常联网权限填入实际授予状态。

这是源码和症状一致的待实测假设，不是本机对用户登录流程的复现。

## 变更

仅对已绑定的微信 user0 正常联网权限 INTERNET / ACCESS_NETWORK_STATE 做宿主身份映射；原始 binder 给出实际允许或拒绝结果，不直接伪造允许。不新增相机、短信、麦克风或定位权限授予。
覆盖 AMS、传统包管理器、Android15权限管理器；保留设备标识参数和原始调用数组，其他包/UID/PID/user不做映射。虚拟包网络权限标志根据宿主真实授予状态填写。
诊断记录访客 Context 的两项联网权限结果；ticket、token、验证码参数和手机号参数在写入及导出时脱敏。

## 验证范围

9项导入策略、24项网络映射策略、6项日志参数脱敏检查通过，共39项。覆盖Android15参数位置、UID/PID/user作用域、参数复制、设备参数保留、敏感权限不介入、真实宿主拒绝标志及回归脱敏。
SDK35 / NDK28c ARM64编译打包通过。最终APK为版本0.1.1、版本代码2，签名验证、ARM64 ELF架构、授权信息及去除自动上传地址的检查通过；签名证书与0.1.0一致，可覆盖升级。
本版仍不修改定位，不表示账号安全检查或验证码登录已成功；需要用户覆盖安装后重新启动访客、从正常登录流程产生新页面进行验收。

## 参考

- https://github.com/ALEX5402/NewBlackbox/issues/68
- https://raw.githubusercontent.com/chromium/chromium/main/android_webview/java/src/org/chromium/android_webview/AwSettings.java
- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-15.0.0_r1/core/java/android/permission/PermissionManager.java
