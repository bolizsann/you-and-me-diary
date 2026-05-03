# You & Me Diary

`You & Me Diary` 是一个面向孕期的私密 AI 陪伴日记。它希望把每天的身体感受、情绪变化、照片和碎片文字，整理成给自己、给宝宝、给未来回看的温柔记录。

当前项目处于 MVP 早期阶段：优先做一个可以在 vivo X100 上运行的原生 Android mock app，先验证首页、记录、生成中、结果、时间线、纪念册和设置页的核心体验，再逐步接入本地存储、图片、语音和 Gemma 生成。

## 当前状态

已完成：

- 第 0 阶段：Android 项目、FastAPI 轻后端空壳、`GET /health`、本地 mock 数据、基础项目文档整理。
- 第 1 阶段：Kotlin + Jetpack Compose + Material 3 的可点击 mock UI 主流程。
- 架构整理：按 `app`、`feature`、`core/designsystem`、`domain/model`、`data/mock` 拆分 Android 代码。
- 本地 mock 体验：Home / Record / Generating / Result / Timeline / Memory Book / Settings。
- 轻量测试：mock repository 单元测试与 Compose smoke test。

待完成：

- 第 2 阶段：Room、DataStore、本地图片保存、收藏持久化、时间线和纪念册从本地数据库读取。
- 第 3 阶段：图卡渲染和分享长图导出。
- 第 4 阶段：FastAPI `/generate-diary` 与云端 Gemma 推理。
- 第 5 阶段：图片选择与语音转文字。

## 产品方向

产品原则来自 `docs/you-and-me-diary-product-design.md`：

- 先接住情绪，再整理内容。
- 温柔、克制，不做医疗诊断。
- 默认私密，不把分享当成主轴。
- 每次记录既服务当下，也沉淀成未来的纪念。

核心体验：

```text
记录今天
  -> 被理解
  -> 生成日记图页
  -> 进入时间线
  -> 收藏到纪念册
  -> 后续导出分享长图
```

## 项目结构

```text
app/                         Android 原生 App
  src/main/java/com/youandme/diary/
    MainActivity.kt          Activity 入口
    app/                     App 级状态与 mock 导航
    feature/                 各页面 UI
    core/designsystem/       共享 Compose 组件
    domain/model/            领域模型
    data/mock/               mock 数据源

backend/                     FastAPI 轻后端
  app.py                     当前提供 GET /health
  requirements.txt

docs/                        产品、技术、构建和计划文档
  prototypes/                HTML 设计原型
```

## Android 开发

推荐使用 Android Studio 打开项目根目录。

环境要求：

- Android Studio，使用默认 Setup Wizard 安装 SDK 和 JDK。
- Android SDK Platform-Tools。
- vivo X100 开启开发者选项和 USB debugging。

常用命令：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

如果当前仓库缺少 `gradlew.bat`，可以先使用 Android Studio 的 Gradle 面板执行同名任务，后续再补齐 Gradle Wrapper 文件。

## Android Studio 预览

首页 Compose Preview 在：

```text
app/src/main/java/com/youandme/diary/feature/home/HomeScreen.kt
```

打开 `HomeScreenPreview` 后，在编辑器右上角切到 `Split` 或 `Design` 即可查看静态预览。Preview 适合快速看 UI，最终仍需要在 vivo X100 上手动验收。

## 后端开发

当前 Android App 默认使用本地 mock 数据，不依赖后端。

FastAPI 只是第 0 阶段的服务边界预留：

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app:app --reload
```

健康检查：

```text
GET http://127.0.0.1:8000/health
```

期望返回：

```json
{ "status": "ok" }
```

后续接入 Gemma 时，FastAPI 会负责临时接收文本和图片、构造 prompt、调用模型、校验 JSON，并返回给 Android 本地保存和渲染。

## 文档入口

- `docs/you-and-me-diary-product-design.md`：产品设计与体验方向。
- `docs/you-and-me-diary-technical-plan.md`：MVP 技术方案和阶段计划。
- `docs/build.md`：构建、预览、真机运行和排错说明。
- `docs/day2-plan.md`：第 2 阶段本地存储计划。
