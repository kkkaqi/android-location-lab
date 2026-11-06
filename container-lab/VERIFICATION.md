# 0.1.0 构建与验证记录

日期：2026-11-03。用户已接受“独立容器先导入/启动微信”的阶段范围。

## 通过的检查

- 9 项纯 Java 导入策略断言通过，包括上游默认 success=true/null 包名不得误报、真实容器安装记录确认，以及 ARM64 宿主/访客架构判断。
- Gradle 8.14.3、现有 Temurin JDK 24、Android SDK 35、Build Tools 35、NDK 28.2.13676358：完整 ARM64 Java/AIDL/C++ 编译和 APK 打包通过。
- apksigner 签名验证通过：本地 Android Debug 证书，v2 签名。
- APK 包名 `app.containerlab`，版本 0.1.0；ARM64 原生引擎 `lib/arm64-v8a/libblackbox.so` 存在，ELF64/AArch64 标识检查通过。
- 上游 Apache-2.0 LICENSE 与来源说明已打入 APK assets，同时随源码保留。
- 在打包的 DEX 中未检测到已移除的自动日志上传服务地址。
- 源码包完整性检查通过，不含签名私钥、SDK、NDK、Gradle 缓存或生成 class 文件。
- 独立只读审查完成，已修复 manifest 合并冲突、包安装状态退回真实系统的误报风险、Application 回调误报、容器失败时退回启动原微信的路径。

## 构建问题及修复

1. 旧 VirtualLocation 的 SDK/target 为25，缺少 ARM64 配置，自带 APK 只有 x86 核心库，不作为交付基础。
2. Windows 中文工作目录导致 AIDL 输出编码错误，构建使用独立英文路径缓存。
3. Windows AIDL 生成的命令注释包含反斜线 user 路径，导致 Java Unicode escape 错误；仅移除这一行生成注释。
4. 独立 host 采用 target SDK 28 以保留容器对旧版 API 的使用方式；不将该实验配置描述为现代应用商店发布配置。

## 尚未验证

没有在用户 OnePlus PKG110、Android 15 / ColorOS 15 的真实设备上安装或启动该原型。
没有确认微信登录页、访客进程稳定性、登录、聊天、小程序或定位兼容性。
本次第一阶段没有接入定位功能，不是原始全局修改定位需求的完成声明。
原生库的上游预编译输入未经过完整机器码安全审核。

签名检查、核心测试与编译通过不能替代这些真机验收。

最终 APK SHA-256 见主项目 dist/container-SHA256SUMS.txt。
