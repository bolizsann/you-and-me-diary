# 项目开发提示

## 构建优先级

每次修改 Android Kotlin、Compose、Gradle 或资源文件后，优先在不连接手机的情况下运行：

```powershell
.\gradlew.bat :app:assembleDebug
```

只有 `assembleDebug` 通过后，再连接 vivo X100 运行或安装：

```powershell
.\gradlew.bat :app:installDebug
```

原因：`Gradle Sync` 只检查工程模型，不会完整编译 Kotlin/Compose 源码；很多代码错误只有 build/run 阶段才会出现。

## 测试

本地单元测试：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

真机或模拟器 UI 测试：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 后端定位

`backend/` 是轻后端/API 层。当前阶段 Android App 使用本地 mock 数据，不依赖后端。后续接入 Gemma 时，FastAPI 用于接收 App 请求、调用 Gemma、返回结构化日记 JSON。
