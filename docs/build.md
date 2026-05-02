# 构建与调试说明

## 推荐开发流程

Android UI 或 Kotlin 代码改完后，先在不连接手机的情况下跑一次本地构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

这个命令会编译 debug APK，但不会安装到手机。它可以提前发现：

- Kotlin 编译错误，例如 `Unresolved reference`
- Compose 类型错误
- Gradle 依赖问题
- Android 资源问题
- Manifest 配置问题

`Gradle Sync` 只能说明工程配置和依赖模型能加载，并不代表 Kotlin/Compose 代码已经编译通过。因此后续开发不要只看 Sync 成功，至少要跑 `assembleDebug`。

## 运行到手机

本地构建通过后，再连接 vivo X100：

1. 手机开启开发者选项。
2. 开启 USB 调试。
3. 使用支持数据传输的 USB 线连接电脑。
4. 手机上允许 RSA 调试授权。
5. Android Studio 顶部设备下拉框选择 vivo X100。
6. 点击绿色 Run 按钮，或运行：

```powershell
.\gradlew.bat :app:installDebug
```

安装成功后，手机上会出现 `You & Me Diary`。

## 在 Android Studio 中预览界面

Compose 页面可以在不连接手机的情况下预览。

当前首页已经在 `app/src/main/java/com/youandme/diary/feature/home/HomeScreen.kt` 里提供了 `HomeScreenPreview`：

```kotlin
@Preview(name = "首页 / 日记封面", showBackground = true, widthDp = 430, heightDp = 880)
```

查看方式：

1. 打开 `app/src/main/java/com/youandme/diary/feature/home/HomeScreen.kt`。
2. 找到文件底部的 `HomeScreenPreview`。
3. 在编辑器右上角切到 `Split` 或 `Design`。
4. 如果 Preview 没自动刷新，点击 Preview 面板里的刷新按钮。
5. 如果提示需要 build，先运行：

```powershell
.\gradlew.bat :app:assembleDebug
```

Preview 适合快速看静态 UI，但它不会完整模拟真机导航、输入法、系统字体差异和设备权限。最终仍需要在 vivo X100 上跑一遍。

## 测试命令

本地单元测试不需要手机：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

真机/模拟器 UI 测试需要设备已连接并授权：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 常见排错

如果看到 `Read timed out`，通常是 Gradle 下载依赖或 wrapper 超时，还没有进入手机安装阶段。先重试 Sync/Build；如果持续失败，再根据超时地址判断是 Gradle、Google Maven 还是 Maven Central 网络问题。

如果 Sync 成功但 Run 失败，优先看 `Build Output` 中第一个 Kotlin/Compose 编译错误。后面的错误经常是级联结果，先修第一个。

## FastAPI 在项目中的位置

FastAPI 属于轻后端/API 层，不属于 Android App 本体。

当前第 1 天版本中，Android App 使用本地 mock 数据，不依赖后端运行。`backend/` 里的 FastAPI 只提供 `/health`，用于证明后续服务边界可以启动和访问。

后续接入 Gemma 时，数据流会变成：

```text
Android App
  -> POST /generate-diary
  -> FastAPI 后端构造 prompt
  -> 后端调用 Gemma
  -> 后端校验并返回结构化 JSON
  -> Android App 本地保存并渲染日记卡
```

也就是说：

- Android 负责界面、本地数据、图片选择、时间线、纪念册和分享图渲染。
- FastAPI 负责临时接收文本/图片、调用 Gemma、做 JSON 校验和安全后处理。
- 服务端不长期保存用户原始日记或图片。
