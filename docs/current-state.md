# 当前状态

更新时间：2026-05-16

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

下一阶段是 Phase 3 / Day 3：Record 重写、图卡渲染、真实图片 media 链路和分享长图导出。整体阶段顺序见 `docs/you-and-me-diary-technical-plan.md`，详细实施计划见 `docs/day3-card-rendering-and-record.md`。

Phase 3 目标：

- 按 `docs/prototypes/record-redesign.html` 重写 Record 页。
- 接入单图 Photo Picker，将图片复制到 app 私有目录并写入 `entry_media`。
- 同一天多次提交聚合到一个 `DiaryEntry`，每次提交追加一张 `DiarySlide`。
- 根据当前 `DiarySlide`、`EntryMedia` 和用户记录数据渲染更接近“日记图页”的视觉结果。
- 完成当前 slide 的分享长图预览和导出能力。
- 保持本地优先：生成结果、收藏和时间线继续走本地数据链路。

## 尚未实现

- Room migration 复杂测试。
- 真实图片保存和真实 media 记录落地使用。
- Photo Picker。
- 语音输入或 speech-to-text。
- 分享图或长图导出。
- FastAPI `POST /generate-diary`。
- Gemma 接入。
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
- `docs/you-and-me-diary-design-decisions.md`：当前 UI 和产品决策。
- `docs/build.md`：构建、预览、真机运行和排错说明。
