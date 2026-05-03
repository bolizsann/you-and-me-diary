# Codex 协作指南

这是本仓库给 AI coding agent 使用的主入口文档。后续如果规则、当前阶段、构建命令或架构边界变化，优先更新这里和 `docs/current-state.md`。

## 项目概况

`You & Me Diary` 是一个面向孕期的私密 AI 陪伴日记。当前 MVP 目标是先做一个能在 vivo X100 上运行的 Android 原生 App；后端只保留很薄的 FastAPI 边界，后续用于接入 Gemma 生成。

当前实现重点：

- Android mock app：Kotlin、Jetpack Compose、Material 3。
- 本地优先数据链路：Room、DataStore、mock 生成落库、收藏持久化。
- FastAPI 后端当前只提供 `GET /health`。
- 下一阶段是 Phase 3：图卡渲染和分享长图导出。

开始修改前，优先阅读：

- `docs/current-state.md`：当前真实项目状态。
- `docs/development-guide.md`：常见任务工作流。
- `docs/you-and-me-diary-design-decisions.md`：产品与 UI 决策。
- `docs/day2-local-storage.md`：处理 Room、DataStore、本地持久化时的阶段计划。

## 产品规则

- 产品气质应保持私密、温柔、克制，像日记，不像管理工具。
- 先接住情绪，再整理内容或给建议。
- 不要把 App 做成医疗工具、社交信息流、母婴电商或数据仪表盘。
- 默认优先本地和私密，除非任务明确涉及后端生成。
- 分享和导出是后续能力，不是当前主流程中心。

安全边界：

- 不输出医疗诊断、治疗方案、药物建议，不能替代医生判断。
- 遇到高风险孕期症状时，只做记录、问题整理和提醒咨询医生。

## 仓库结构

```text
app/                         Android App
  src/main/java/com/youandme/diary/
    MainActivity.kt          Activity 入口
    app/                     App 级状态和 enum mock 导航
    feature/                 Compose 页面
    core/designsystem/       共享 Compose UI 组件和工具
    domain/model/            UI/domain 模型
    data/mock/               当前 mock repository 和收藏状态

backend/                     FastAPI API 边界
  app.py                     当前 GET /health
  test_app.py                后端 smoke test

docs/                        产品、技术、构建和阶段文档
```

新增 Android 代码时，遵守现有包结构。本地存储相关代码放到 `data/local`、`data/settings` 和 repository；App 级状态由 `DiaryAppViewModel` 持有，不要继续把业务状态堆进 `YouAndMeDiaryApp`。

## 构建与测试

从仓库根目录使用 Gradle Wrapper：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

如果当前 shell 找不到 `java`，使用 Android Studio 自带 JBR：

```powershell
$env:JAVA_HOME='D:\software\Android\Android Studio\jbr'
```

构建优先级：

1. 修改 Android Kotlin、Compose、Gradle、Manifest 或资源文件后，先跑 `.\gradlew.bat :app:assembleDebug`。
2. 修改 data、domain、repository 或业务状态后，也跑 `.\gradlew.bat :app:testDebugUnitTest`。
3. 只有在设备或模拟器已连接并授权后，再跑 `.\gradlew.bat :app:installDebug` 或 `.\gradlew.bat :app:connectedDebugAndroidTest`。

后端命令：

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
pytest
uvicorn app:app --reload
```

## 实现边界

- 保持改动聚焦在当前任务或当前阶段，不做无关重构。
- 不要替换当前 enum mock 导航，除非任务明确需要。
- 添加持久化时，保持 `domain/model` 与 Room entity 分离。
- UI 使用 domain model，不直接依赖数据库 entity。
- mock 数据和 UI 文案要保持当前产品语气。
- MVP 早期阶段避免大面积架构重写。
- 阶段状态、文件路径、命令或关键决策变化时，同步更新文档。

## 当前阶段提示

Phase 0、Phase 1 和 Phase 2 已完成到可继续迭代的程度。下一步 Phase 3 是图卡渲染和分享长图导出：

- 结果页继续以日记图页为主体验。
- 优先做稳定的本地渲染，不引入真正图像生成。
- 复用当前 `DiarySlide`、`DiaryNote`、收藏和时间线数据链路。
- 后续再接真实 Photo Picker、语音输入和 Gemma API。
