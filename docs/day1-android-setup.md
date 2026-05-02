# 第 1 天 Android 环境搭建

当前项目已经可以导入 Android Studio。创建第 1 天工程文件时，这台机器尚未安装 Java、Gradle、Android Studio、ADB 或 Android SDK，因此 Android 构建和真机安装需要在完成本地环境安装后继续。

## 安装 Android Studio

1. 从 Android 官方网站下载 Windows 版 Android Studio。
2. 运行安装程序，保留默认组件。
3. 第一次启动时使用默认安装向导，让 Android Studio 自动安装 JDK、Android SDK、SDK Platform 和 Android SDK Platform-Tools。
4. 在 Android Studio 中以已有项目方式打开本文件夹。
5. 如果 Android Studio 询问是否信任项目，选择信任。
6. 等待 Gradle Sync 完成。

本仓库中的 Gradle wrapper 配置指向 Gradle `9.4.1`。如果 Android Studio 提示缺少 wrapper JAR，选择 Android Studio 提供的生成/修复 Gradle wrapper 操作；或者在已安装 Gradle 的环境中运行 `gradle wrapper`。

## vivo X100 USB 调试

1. 在手机上开启开发者选项。
2. 开启 USB 调试。
3. 使用支持数据传输的 USB 线连接手机和电脑。
4. 在手机上接受 RSA 调试授权弹窗。
5. 在 Android Studio 中选择 vivo 设备，然后点击 Run。
6. 如果设备不可见，使用 `Tools > Troubleshoot Device Connections` 排查；Windows 下如仍无法识别，安装 vivo/OEM USB 驱动。

## 环境安装后的检查命令

```powershell
java -version
adb devices
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

其中 `connectedDebugAndroidTest` 需要已连接并授权的 vivo X100，或已启动的 Android 模拟器。
