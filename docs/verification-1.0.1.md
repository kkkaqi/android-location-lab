# 1.0.1 诊断更新验收记录

用户实测：一加 Ace 5 / ColorOS 15，百度地图先显示武汉，约十秒后显示实际位置，定点仍显示运行；微信小程序也有定位问题。这是用户观察，不是本机复现。现阶段无法据此确认根因。

旧版只显示成功调用注入接口，虽然记录了单调更新时间，但界面没有使用该时间检查延迟，也没有回读系统位置。本次补足诊断信息，不宣称已修复目标应用的定位兼容性。

本次验证：34 项纯 Java 核心断言通过；包括新增更新时间、最大中断间隔、过期缓存、模拟标记和目标偏离的检查。UI 新鲜度判断复用已测试的 UpdateHealth 方法。SDK 35 编译、DEX 转换、zipalign 与 APK 签名校验通过，版本代码 2、版本名 1.0.1。独立只读检查未发现阻塞问题。

读取 getLastKnownLocation 是系统缓存检查，不会请求新的传感器定位；回读显示其时间及模拟标记，超过五秒的缓存不作为持续注入成功证据。即使回读与目标相符，也不等同于微信或百度使用了该结果。

每五轮左右将诊断快照保存到本机应用私有存储；显示最近间隔，用户明确点击后才将诊断复制到剪贴板。报告包含设备型号、系统构建、目标坐标、省电状态和来源分类，不包含微信内容或回读实际坐标，没有网络上传。

仍需用户手机执行：安装覆盖升级，开始模拟，进入地图等待出现回跳，立即切回定点，点击复制运行诊断。基于回读新鲜度、mock 标记、更新间隔以及目标应用表现继续定位原因。没有执行真机 UI、ColorOS 后台行为或微信/百度兼容性验证。

参考：
- [Android last-known-location 缓存说明](https://developer.android.com/reference/android/location/LocationManager#getLastKnownLocation(java.lang.String))
- [百度 SDK 网络和设备定位模式](https://lbsyun.baidu.com/docs/android?title=android-locsdk%2Fguide%2Fget-location%2Flatlng)
- [百度 SDK 模拟位置过滤能力](https://lbs.baidu.com/docs/android?title=android-locsdk%2Fguide%2Faddition-func%2Fmock-location)

上述 SDK 文档只能证明这些能力存在，不能证明用户当前百度地图或微信小程序确切使用了哪个配置。
