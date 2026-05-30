# You & Me Diary

`You & Me Diary` 是一个面向孕期的私密 AI 陪伴日记。它希望把每天的身体感受、情绪变化、照片和碎片文字，整理成给自己、给宝宝、给未来回看的温柔记录。

当前真实状态以 `docs/current-state.md` 为准。当前 App 已具备本地日记链路、后端 `/generate-diary`、Cloud Run 部署和手机端云端调用能力，后续会继续推进端侧 local Gemma client、语音输入和分享导出细节。

## 当前状态

已完成：

- 第 0 阶段：Android 项目、FastAPI 轻后端、`GET /health`、本地 mock 数据、基础项目文档整理。
- 第 1 阶段：Kotlin + Jetpack Compose + Material 3 的可点击 mock UI 主流程。
- 架构整理：按 `app`、`feature`、`core/designsystem`、`domain/model`、`data/mock` 拆分 Android 代码。
- 本地 mock 体验：Home / Record / Generating / Result / Timeline / Memory Book / Settings。
- 轻量测试：mock repository、mapper、DAO 与 Compose smoke test。
- 构建入口：已补齐 Gradle Wrapper，可从项目根目录使用 `.\gradlew.bat`。
- 第 2 阶段：Room、DataStore、mock 生成落库、收藏持久化、note 编辑保存、时间线和纪念册本地读取。

近期继续：

- 分享长图导出体验细节。
- 端侧 local Gemma client 与 online/local 选择。
- 语音输入或 speech-to-text。

## 产品方向

产品原则来自 `docs/product-design.md`：

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
    data/local/              Room 本地数据层
    data/mock/               mock 数据源
    data/settings/           DataStore 设置持久化

backend/                     FastAPI 轻后端
  app.py                     FastAPI 路由
  online_gemma_client.py     云端 Gemma 调用
  baby_reply_policy.py       宝宝回复策略
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

如果当前 shell 找不到 `java`，可以先临时使用 Android Studio 自带 JBR：

```powershell
$env:JAVA_HOME='D:\software\Android\Android Studio\jbr'
```

## Android Studio 预览

首页 Compose Preview 在：

```text
app/src/main/java/com/youandme/diary/feature/home/HomeScreen.kt
```

打开 `HomeScreenPreview` 后，在编辑器右上角切到 `Split` 或 `Design` 即可查看静态预览。Preview 适合快速看 UI，最终仍需要在 vivo X100 上手动验收。

## 后端开发

当前 Android App 可通过 `backendBaseUrl` 调用后端；默认测试配置指向 Cloud Run，仍保留本地 fallback。

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

FastAPI 负责临时接收文本和图片、构造 prompt、调用模型、校验 JSON，并返回给 Android 本地保存和渲染。

## 文档入口

- `AGENTS.md`：Codex/agent 协作规则入口。
- `docs/current-state.md`：当前真实项目状态。
- `docs/development-guide.md`：常见开发任务工作流。
- `docs/product-design.md`：产品设计与体验方向。
- `docs/technical-plan.md`：MVP 技术方案和阶段计划。
- `docs/build.md`：构建、预览、真机运行和排错说明。
- `docs/design-decisions.md`：产品和 UI 决策。
- `docs/features/day2-local-storage.md`：第 2 阶段本地存储计划。
- `docs/features/day4-backend-gemma.md`：第 4 阶段后端生成链路。
