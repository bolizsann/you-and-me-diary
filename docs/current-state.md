# 当前状态

更新时间：2026-06-03

这份文档只记录仓库当前真实状态。开始规划或实现新任务前，先看这里；阶段计划和历史记录放在 `docs/features/`。

## 产品方向

`You & Me Diary` 是一个面向孕期的私密 AI 陪伴日记。MVP 核心闭环是：

```text
记录今天
  -> 被理解
  -> 生成日记图页
  -> 进入时间线
  -> 收藏到纪念册
  -> 后续导出分享长图
```

产品气质应保持温柔、私密、非医疗。它帮助用户记录、理解和整理感受与回忆，不做孕期症状诊断。

## 已完成

- Phase 0 项目准备：
  - Android App 模块位于 `app/`。
  - FastAPI 后端位于 `backend/`。
  - 基础项目文档位于 `docs/`。
- Phase 1 可点击 mock app：
  - Kotlin、Jetpack Compose、Material 3。
  - 已有 Home / Record / Generating / Result / Timeline / Memory Book / Settings。
  - 领域模型位于 `domain/model`。
- Phase 2 本地存储与持久化：
  - Room entity、DAO、database、relation 和 mapper 位于 `data/local`。
  - `DiaryRepository` 负责 mock seed、创建记录、收藏切换和 note 编辑保存。
  - DataStore 设置仓库位于 `data/settings`。
  - 用户名、预产期和主题持久化。
  - Timeline 和 Memory Book 读取本地数据链路。
- Phase 3 图卡和记录链路：
  - Record 支持文字与本地图片附件。
  - Result 以日记图页为主体验。
  - 时间线、收藏、编辑 note 和本地落库链路已打通。
- Phase 4 后端生成链路：
  - FastAPI 提供 `GET /health`、`GET /version`、`POST /generate-diary`。
  - `POST /generate-diary` 使用 `X-App-Token` 保护。
  - 后端构造 prompt，调用 Google 托管模型，返回结构化 JSON。
  - API key 缺失、模型失败、解析失败时返回 fallback，App 主流程不断。
  - Android Record 提交优先调用后端，失败时保留本地 fallback。
  - 图片请求会上传用户选定 ROI 裁切后的压缩图。
- Phase 5 Cloud Run 与宝宝回复策略：
  - FastAPI 已部署到 Cloud Run。
  - Secret Manager 管理 `GEMINI_API_KEY` 和 `APP_API_TOKEN`。
  - Android 默认 `backendBaseUrl` 指向 Cloud Run，token 通过 Gradle property 注入，不提交到仓库。
  - `/version` 返回 `apiVersion`、当前模型、`gitSha`、`sourceBuildStamp`，用于确认线上 revision 对应的代码版本。
  - 宝宝回复策略已从模型输出后处理：快乐/中性降低文本频率，悲伤/疲惫/胎动提高文本频率，轻回复池扩展到每个 mode 至少 10 个 emoji 和 30 个轻状态。
- Phase 6 语音准备与端侧 Gemma 验证：
  - Android 已接入 LiteRT-LM `LocalGemmaClient`，读取 app-specific external files 目录里的 `gemma-4-E2B-it.litertlm`。
  - Settings 开发工具区新增 `Offline / Online` 生成模式开关，默认 `offline`。
  - 为语音最小可用版提前完成 Android 生成链路重构：`DiaryAppViewModel` 只构造 `DiaryGenerationRequest`，由 `DiaryGenerationGateway` 按 `generationMode` 分发到 online 或 offline 实现。
  - `RemoteDiaryGenerator` 包装 `RemoteGemmaClient` 并负责 remote 图片 base64 请求；`LocalDiaryGenerator` 包装 `LocalGemmaClient` 并负责 local 临时裁剪图清理。
  - 图片 ROI 裁切、384px JPEG 压缩和主色估算集中在 `DiaryGenerationImageProcessor`，ViewModel 不再持有 remote/local 专用图片处理逻辑。
  - online/offline 生成失败时仍返回 `null`，由 `DiaryRepository` 保留现有本地 fallback，主流程不断。
  - Local 生成已增加与 online 接近的后处理策略：`preserve` 模式保留用户原文，`babyText` 对齐后端情绪分类、阈值和轻状态池，并记录本地推理耗时日志。
  - APK 已打包 LiteRT-LM v0.12.0 Android arm64 GPU sampler 预编译库：`libLiteRtTopKOpenClSampler.so`、`libLiteRtTopKWebGpuSampler.so`。
  - vivo X100 / V2548A 真机真实 Record UI 链路已验证图片+文本推理，结果 source 为 `local-gemma-gpu`。
  - 真机热启动图片+文本推理最近稳定约 5.5-6.9s，提交链路日志保留 `initMs`、`inferenceMs`、`totalMs` 用于后续性能排查。
  - 当前模型路径：`/sdcard/Android/data/com.youandme.diary/files/models/gemma-4-E2B-it.litertlm`。
- 构建工具：
  - 已补齐 Gradle Wrapper：`gradlew.bat`、`gradlew`、`gradle/wrapper/`。
  - Wrapper 指定 Gradle `9.4.1`。

## 当前后端部署

Cloud Run：

```text
project: gen-lang-client-0823926280
region: asia-east1
service: you-and-me-diary-api
latest verified revision: you-and-me-diary-api-00009-g6t
verified gitSha: 52a042e
```

主要 URL：

```text
https://you-and-me-diary-api-7ofcf3aymq-de.a.run.app
https://you-and-me-diary-api-265810336333.asia-east1.run.app
```

部署后用 `GET /version` 确认 `gitSha` 与本地 commit 一致。

## 进行中 / 下一步

当前重点：

- 继续稳定云端生成体验和宝宝回复策略。
- 继续验证端侧 Gemma 在真实 Record UI 链路里的首 token 延迟、总耗时、发热和输出稳定性。
- 继续处理语音输入或 speech-to-text。
- 继续完善分享长图导出体验，尤其是 `宝宝说` 为空时的布局。

## 尚未实现

- 语音输入或 speech-to-text。
- Room migration 复杂测试。
- 分享长图导出仍有已知布局问题：`宝宝说` 为空时可能保留不必要空白。
- 账号系统、云同步或生产级后端存储。

## 当前架构

```text
app/
  src/main/java/com/youandme/diary/
    MainActivity.kt
    app/
      AppScreen.kt
      DiaryAppViewModel.kt
      YouAndMeDiaryApp.kt
    feature/
      home/
      record/
      generating/
      result/
      timeline/
      memory/
      settings/
    core/designsystem/
    domain/model/
    data/local/
    data/localai/
    data/generation/
    data/mock/
    data/remote/
    data/settings/

backend/
  app.py
  schemas.py
  prompt.py
  online_gemma_client.py
  baby_reply_policy.py
  diary_fallbacks.py
  generation_settings.py
  benchmark_gemma_latency.py
  test_app.py
  Dockerfile
  requirements.txt

docs/
  current-state.md
  development-guide.md
  design-decisions.md
  product-design.md
  technical-plan.md
  build.md
  features/
  assets/
  prototypes/
```

## 验证命令

Android：

```powershell
$env:JAVA_HOME='D:\software\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

只有在设备或模拟器已连接并授权后，才运行：

```powershell
.\gradlew.bat :app:installDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

端侧 Gemma 验证以真实 Record UI 流程为准：确认手机 app-specific external files 目录存在模型文件，Settings 切到 `Offline`，提交图片+文本记录后查看 `Local generation completed` 日志中的 `initMs`、`inferenceMs`、`totalMs`。

Android 生成链路：

```text
DiaryAppViewModel
  -> DiaryGenerationRequest
  -> DiaryGenerationGateway
      -> RemoteDiaryGenerator -> RemoteGemmaClient -> Cloud Run /generate-diary
      -> LocalDiaryGenerator  -> LocalGemmaClient  -> LiteRT-LM local Gemma
  -> GeneratedDiaryDraft?
  -> DiaryRepository.createOrAppendTodayEntry(...)
```

当前职责边界：

- `DiaryAppViewModel`：收集 UI 状态、计算日期、`inputSource`、`diaryTextMode`、当天第一条状态，调用 gateway，落库和导航。
- `DiaryGenerationGateway`：按 `GenerationModes.Online / Offline` 选择生成实现，并提供 local warm-up 入口。
- `RemoteDiaryGenerator` / `LocalDiaryGenerator`：把统一 request 适配为 remote/local client 请求。
- `DiaryGenerationImageProcessor`：集中处理 ROI、JPEG 压缩、remote base64、local 临时图片和主色估算。

后端：

```powershell
python -m pytest backend
python -m py_compile backend/benchmark_gemma_latency.py backend/online_gemma_client.py backend/baby_reply_policy.py backend/diary_fallbacks.py backend/generation_settings.py
```

Cloud Run 版本确认：

```powershell
Invoke-RestMethod -Uri 'https://you-and-me-diary-api-7ofcf3aymq-de.a.run.app/version'
```

## 来源文档

- `docs/technical-plan.md`：整体 MVP 计划和阶段顺序。
- `docs/product-design.md`：产品设计与体验方向。
- `docs/design-decisions.md`：当前 UI、产品和关键策略决策。
- `docs/development-guide.md`：常见开发任务工作流。
- `docs/build.md`：构建、预览、真机运行和排错说明。
- `docs/features/day1-android-setup.md`：Phase 0 / Day 1 Android 项目准备。
- `docs/features/day2-local-storage.md`：Phase 2 本地存储计划和历史验收参考。
- `docs/features/day3-card-rendering-and-record.md`：Phase 3 Record、media、图卡和分享长图计划。
- `docs/features/day4-backend-gemma.md`：Phase 4 后端生成、Gemma 和 prompt 计划。
- `docs/features/day5-cloud-run-and-baby-reply-policy.md`：Cloud Run、版本确认和宝宝回复策略。
- `docs/features/day6-voice-and-generation-gateway.md`：语音输入前置准备、Android generation gateway 和端侧/云端生成链路。
