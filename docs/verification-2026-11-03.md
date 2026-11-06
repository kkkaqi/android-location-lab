# 验证记录（2026-11-03）

交付目标为 Android 15 免 Root 固定模拟定位测试 APK。

已执行：

- `tools/test.ps1`：26 项核心断言通过，包括输入边界、无效坐标、首帧、持续更新、停止幂等、启动回滚、部分来源失败、更新失败、清理异常，以及 Android 15 静默忽略未授权请求的回归场景。
- 回归证明：新增“运行中撤销授权”检查在修复前失败，修复后通过。检查代码先于修复编写。
- Android SDK / Build Tools 35：aapt2 资源编译与链接、javac 编译、D8 dex 转换、zipalign 对齐检查均通过。
- apksigner：签名验证通过，使用本地测试证书；不是应用商店发布证书。
- aapt manifest：包名 `app.fixedlocation`，版本 1.0.0，min SDK 31，target SDK 35，location 类型前台服务未导出。无联网、后台位置和开机自启权限。
- 独立只读代码检查发现 AppOp 撤销后可能误报运行的问题，已修复并复查，无其他具体阻塞问题。
- `adb devices -l`：无连接设备。

未执行：真实 Android 手机上的界面、厂商后台策略、微信发送位置和共享实时位置验收；Gradle 路径没有实际构建。不得将核心测试或 APK 编译通过解释为这些场景已通过。

构建环境：现有 Temurin JDK 24；工具链局部存放在 `.tooling/`，未修改全局 JDK/SDK 配置。JDK 24 对 Android 签名工具的本地库调用给出未来兼容性警告，签名验证当前通过。

最终 APK 的 SHA-256 以 `dist/SHA256SUMS.txt` 为准。安装和真机验收步骤见根目录 README。
