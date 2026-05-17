# 当前状态

更新时间：2026-05-18

这份文档只记录仓库当前真实状态。开始规划或实现新任务前，先看这里。

## 产品方向

`You & Me Diary` 是一个面向孕期的私密 AI 陪伴日记。MVP 首先要验证核心闭环：

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
- 后端提供 `GET /health`。
- Day 4 后端生成链路已开始接入：FastAPI 新增 `POST /generate-diary`、独立 prompt builder、Gemma client 和 fallback 结构。
  - 基础项目文档位于 `docs/`。
- Phase 1 可点击 mock app：
  - 使用 Kotlin、Jetpack Compose、Material 3。
  - 已有 Home / Record / Generating / Result / Timeline / Memory Book / Settings。
  - 领域模型位于 `domain/model`。
  - 已有 mock repository 单元测试和 App smoke test。
- Phase 2 本地存储与持久化：
  - 已添加 Room entity、DAO、database、relation 和 mapper，位于 `data/local`。
  - 已添加 `DiaryRepository`，负责 mock seed、创建 mock entry、收藏切换和 note 编辑保存。
  - 已添加 DataStore 设置仓库，位于 `data/settings`。
  - 用户名、预产期和主题由 DataStore 持久化。
  - 新建 mock 日记记录、收藏状态和 note 编辑结果写入本地数据库。
  - App 级业务状态已迁移到 `DiaryAppViewModel`。
  - Timeline 和 Memory Book 读取本地数据链路。
- 构建工具：
  - 已补齐 Gradle Wrapper：`gradlew.bat`、`gradlew`、`gradle/wrapper/`。
  - Wrapper 指定 Gradle `9.4.1`。

## 进行中 / 下一步

当前阶段是 Phase 4 / Day 4：后端 `/generate-diary`、Gemma 4 调用、可调 prompt 和 Android 调用链路。整体阶段顺序见 `docs/you-and-me-diary-technical-plan.md`，Day 3 记录见 `docs/day3-card-rendering-and-record.md`，Day 4 计划与实施记录见 `docs/day4-backend-gemma.md`。

Day 4 目标：

- Android Record 提交优先调用 FastAPI `/generate-diary`。
- 后端构造可调 prompt，调用 Google 托管 Gemma 4，返回结构化 JSON。
- 后端失败、API key 缺失或解析失败时返回 fallback，Android 也保留本地 mock fallback。
- 图片请求使用用户选定 ROI 裁切后的压缩图。
- Phase 5 剩余语音输入功能暂未实现，只在 API 和 prompt 中预留 `voiceText`。

## 尚未实现

- Room migration 复杂测试。
- 语音输入或 speech-to-text。
- 分享长图导出仍有已知布局问题：`宝宝说` 为空时可能保留不必要空白。
- 真实 GEMINI_API_KEY 下的端到端 Gemma 调用验证。
- Cloud Run 部署。
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
    data/settings/

backend/
  app.py
  test_app.py
  requirements.txt
```

Android 当前使用本地 Room/DataStore 保存 mock 生成结果和用户设置。后端只是预留的服务边界，运行 App 不依赖后端。

## 验证命令

从仓库根目录运行：

```powershell
$env:JAVA_HOME='D:\software\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

只有在设备或模拟器已连接并授权后，才运行 `connectedDebugAndroidTest`。

后端验证：

```powershell
cd backend
pytest
```

## 来源文档

- `docs/you-and-me-diary-technical-plan.md`：整体 MVP 计划和阶段顺序。
- `docs/day2-local-storage.md`：Phase 2 本地存储计划和历史验收参考。
- `docs/day3-card-rendering-and-record.md`：Phase 3 / Day 3 Record、media、图卡和分享长图计划。
- `docs/day4-backend-gemma.md`：Phase 4 / Day 4 后端生成、Gemma 和 prompt 计划。
- `docs/you-and-me-diary-design-decisions.md`：当前 UI 和产品决策。
- `docs/build.md`：构建、预览、真机运行和排错说明。
