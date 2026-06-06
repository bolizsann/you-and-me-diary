# 构建与调试说明

## 推荐开发流程

Android UI 或 Kotlin 代码改完后，先在不连接手机的情况下跑一次本地构建：

如果当前 shell 找不到 `java`，先使用 Android Studio 自带 JBR：

```powershell
$env:JAVA_HOME='D:\software\Android\Android Studio\jbr'
```

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

如果要测试 `Online` 生成，debug 包必须在构建/安装时注入后端 URL 和 token；否则
`BuildConfig.BACKEND_APP_TOKEN` 会是空字符串，`POST /generate-diary` 会返回 401。
不要把 token 写进仓库。推荐把后端 URL 和 token 写入被 `.gitignore` 忽略的 `local.properties`，
这样 Android Studio Run 和命令行 `installDebug` 都能稳定读取：

```powershell
$env:CLOUDSDK_CONFIG='D:\software\gcloud-config'
$token = (D:\software\google-cloud-sdk\bin\gcloud.cmd secrets versions access latest --secret=APP_API_TOKEN).Trim()
$url = 'https://you-and-me-diary-api-265810336333.asia-east1.run.app'
$path = 'local.properties'
$content = if (Test-Path $path) { Get-Content $path -Raw } else { '' }
if ($content -match '(?m)^backendBaseUrl=') {
  $content = $content -replace '(?m)^backendBaseUrl=.*$', "backendBaseUrl=$url"
} else {
  $content = $content.TrimEnd() + "`r`nbackendBaseUrl=$url`r`n"
}
if ($content -match '(?m)^backendAppToken=') {
  $content = $content -replace '(?m)^backendAppToken=.*$', "backendAppToken=$token"
} else {
  $content = $content.TrimEnd() + "`r`nbackendAppToken=$token`r`n"
}
Set-Content -Path $path -Value $content -NoNewline
.\gradlew.bat :app:installDebug
```

如果只想做一次性命令行安装，可以改用环境变量，避免 PowerShell 引号和特殊字符影响
`-PbackendBaseUrl=...` / `-PbackendAppToken=...`：

```powershell
$env:CLOUDSDK_CONFIG='D:\software\gcloud-config'
$env:BACKEND_BASE_URL = 'https://you-and-me-diary-api-265810336333.asia-east1.run.app'
$env:BACKEND_APP_TOKEN = (D:\software\google-cloud-sdk\bin\gcloud.cmd secrets versions access latest --secret=APP_API_TOKEN).Trim()
.\gradlew.bat :app:installDebug
```

历史排查结论：如果 Online 模式一直 fallback，并且 logcat 里看到 Cloud Run 返回
`401 Invalid app token`，通常不是远端 URL “获取 token” 出错。Cloud Run 的 token 来自
Secret Manager；Android App 的 token 是构建 APK 时写入 `BuildConfig.BACKEND_APP_TOKEN`。
此前出现过命令行 `-PbackendAppToken="$env:APP_API_TOKEN"` 漏传或被 Android Studio Run
绕过的情况，导致新装 APK 内 token 为空或不是预期值。也出现过普通增量安装沿用旧
`backendBaseUrl` 编译产物的情况。稳定做法是把 URL 和 token 都放进本机
`local.properties`，或使用 `BACKEND_BASE_URL` / `BACKEND_APP_TOKEN` 环境变量，再重新构建/安装。

如果确认 `local.properties` 已经正确，但手机仍然 Online fallback，优先排查是否装到了旧的
Gradle 缓存产物。改过 `backendBaseUrl`、`backendAppToken` 或相关 Gradle 配置后，可以强制
全量安装一次：

```powershell
$env:JAVA_HOME='D:\software\Android\Android Studio\jbr'
.\gradlew.bat clean :app:installDebug --no-build-cache --rerun-tasks
```

日常安装优先使用 `installDebug`，它会覆盖 APK 但通常保留 app 数据。不要使用
`adb uninstall`、`adb shell pm clear com.youandme.diary`，或会清理 app 数据目录的测试流程，
除非当前目标就是重置本地数据。做端侧模型验证或 instrumentation 前，先确认是否需要备份
Room 数据库、`files/entry_media/` 和 `/sdcard/Android/data/com.youandme.diary/files/models/`。

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

端侧 LiteRT-LM GPU sampler 使用官方 v0.12.0 Android arm64 预编译库，打包在：

```text
app/src/main/jniLibs/arm64-v8a/libLiteRtTopKOpenClSampler.so
app/src/main/jniLibs/arm64-v8a/libLiteRtTopKWebGpuSampler.so
```

来源：

```text
https://github.com/google-ai-edge/LiteRT-LM/tree/v0.12.0/prebuilt/android_arm64
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
