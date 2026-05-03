# 开发指南

这份文档把常见任务整理成可重复的工作流，帮助后续 agent 保持一致。

## 开始前

1. 先读 `docs/current-state.md`。
2. 做产品或 UI 任务时，读 `docs/you-and-me-diary-design-decisions.md`。
3. 做本地持久化相关任务时，读 `data/local`、`data/settings` 和 `docs/day2-local-storage.md`。
4. 新增结构前，先检查现有包和文件命名。

## Android UI 工作流

适用于 Compose 页面、design system、字符串、主题或导航状态调整。

1. 保持当前温柔、私密、日记感的产品语气。
2. 优先复用 `core/designsystem` 里的组件。
3. 页面专属 UI 放在 `feature/<screen>`。
4. 除非任务需要，不做大规模导航重写。
5. 验证命令：

```powershell
$env:JAVA_HOME='D:\software\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

如果改动涉及交互或状态，也运行：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## 本地存储工作流

适用于 Room 和 DataStore 相关任务。

1. Room 数据库相关代码放在 `app/src/main/java/com/youandme/diary/data/local/`。
2. DataStore 设置相关代码放在 `app/src/main/java/com/youandme/diary/data/settings/`。
3. Room entity 与 `domain/model` 分离。
4. 添加 mapper，在 entity model 和 domain model 之间转换。
5. 添加 repository API，让 UI 和状态持有层不要直接调用 DAO。
6. App 级业务状态优先放在 `DiaryAppViewModel`，`YouAndMeDiaryApp` 只负责组合 UI 和转发事件。
7. 在 Gemma 阶段开始前，继续使用 mock 生成内容。
8. 验证命令：

```powershell
$env:JAVA_HOME='D:\software\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

测试重点根据改动范围选择：mapper、repository、DAO、DataStore、关键页面流程。不要把阶段验收清单塞进本指南；阶段目标应放在对应阶段文档里。

## 后端工作流

适用于 FastAPI 修改。

1. 后端保持轻量：API、prompt 构造、模型调用、响应校验、安全后处理。
2. 不长期保存用户原始日记文本或孕期照片。
3. 新增结构化响应时使用 Pydantic schema。
4. 验证命令：

```powershell
cd backend
pytest
```

新增 `POST /generate-diary` 时，先返回稳定 mock JSON，再接入真实 Gemma。

## 文档工作流

以下内容变化时，需要更新文档：

- 当前阶段状态。
- 构建或测试命令。
- 文件路径。
- 产品决策。
- 架构边界。
- 已知限制或暂缓内容。

真实状态写入 `docs/current-state.md`，计划写入对应阶段文档。

## 完成标准

Android 代码改动：

- 相关构建或测试命令通过。
- UI 仍符合当前产品语气。
- 没有无关重构或文件 churn。
- 如果路径、命令或阶段状态变化，已同步文档。

纯文档改动：

- 链接和文件名对应真实文件。
- 不保留重复的规则入口。
- README 指向正确的当前文档。
