# Codex 协作指南

这是本仓库给 AI coding agent 使用的主入口文档。它只记录稳定协作规则、架构边界和常用命令；真实项目状态、当前阶段和最新路径以 `docs/current-state.md` 为准。

开始修改前，根据任务范围读取：

- `docs/current-state.md`：当前真实项目状态、已完成能力、下一步和文档入口。
- `docs/development-guide.md`：常见任务工作流。
- `docs/design-decisions.md`：产品与 UI 决策。
- `docs/features/day2-local-storage.md`：Room、DataStore、本地持久化相关阶段记录。
- `docs/features/day4-backend-gemma.md`：后端生成、Gemma、prompt 和 Android 远程调用链路。

## 产品规则

- 产品气质应保持私密、温柔、克制，像日记，不像管理工具。
- 先接住情绪，再整理内容或给建议。
- 不要把 App 做成医疗工具、社交信息流、母婴电商或数据仪表盘。
- 默认优先本地和私密，除非任务明确涉及后端生成、Cloud Run 或模型调用。
- 分享和导出是后续能力，不是当前主流程中心。

安全边界：

- 不输出医疗诊断、治疗方案、药物建议，不能替代医生判断。
- 遇到高风险孕期症状时，只做记录、问题整理和提醒咨询医生。

## 仓库结构约定

```text
app/                         Android App
  src/main/java/com/youandme/diary/
    app/                     App 级状态和 enum mock 导航
    feature/                 Compose 页面
    core/designsystem/       共享 Compose UI 组件和工具
    domain/model/            UI/domain 模型
    data/local/              Room 本地数据层
    data/remote/             后端生成接口调用
    data/settings/           DataStore 设置

backend/                     FastAPI API 边界
  app.py                     FastAPI 路由
  schemas.py                 Pydantic schema
  prompt.py                  Prompt builder
  online_gemma_client.py     云端 Gemma 调用
  baby_reply_policy.py       宝宝回复频率与情绪策略
  diary_fallbacks.py         本地 fallback 文案
  generation_settings.py     生成参数

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
