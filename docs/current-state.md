# 当前状态

更新时间：2026-05-30

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
- Phase 4.5 Cloud Run 与宝宝回复策略：
  - FastAPI 已部署到 Cloud Run。
  - Secret Manager 管理 `GEMINI_API_KEY` 和 `APP_API_TOKEN`。
  - Android 默认 `backendBaseUrl` 指向 Cloud Run，token 通过 Gradle property 注入，不提交到仓库。
  - `/version` 返回 `apiVersion`、当前模型、`gitSha`、`sourceBuildStamp`，用于确认线上 revision 对应的代码版本。
  - 宝宝回复策略已从模型输出后处理：快乐/中性降低文本频率，悲伤/疲惫/胎动提高文本频率，轻回复池扩展到每个 mode 至少 10 个 emoji 和 30 个轻状态。
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
- 规划端侧 `local_gemma_client`，让用户后续可以选择 online client 或 local client。
- 继续处理语音输入或 speech-to-text。
- 继续完善分享长图导出体验，尤其是 `宝宝说` 为空时的布局。

## 尚未实现

- 端侧 local Gemma client。
- online/local 生成方式选择。
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
- `docs/features/day4-5-cloud-run-and-baby-reply-policy.md`：Cloud Run、版本确认和宝宝回复策略。
