# 开发指南

这份文档把常见任务整理成可重复的工作流，帮助后续 agent 保持一致。

## 开始前

1. 先读 `docs/current-state.md`。
2. 做产品或 UI 任务时，读 `docs/design-decisions.md`。
3. 做本地持久化相关任务时，读 `data/local`、`data/settings` 和 `docs/features/day2-local-storage.md`。
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

## Gradle 缓存位置

Windows 的 `gradlew.bat` 已在脚本内设置默认 `GRADLE_USER_HOME`。如果外部环境没有主动指定该变量，当前仓库会把 Gradle wrapper、依赖和构建缓存放到 D 盘软件缓存目录：

```text
D:\software\.gradle
```

这样可以避免缓存继续写入 `C:\Users\<用户名>\.gradle`。如果需要清理旧缓存，可以先关闭 Android Studio，再删除用户目录下的 `.gradle`；之后从本仓库运行 `.\gradlew.bat ...` 会重新使用 D 盘缓存。不要把 `GRADLE_USER_HOME` 指向仓库内的 `.gradle/`，那个目录更适合作为 project-local cache，已经被 git 忽略。

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

## Compose UI 测试注意事项

`connectedDebugAndroidTest` 会在真机或模拟器上真实安装、启动和点击 App。它比 JVM 单测慢，也更容易受页面滚动、软键盘、异步状态和语义树选择器影响。写 UI 测试时优先验证用户关键路径，但测试动作必须稳定。

1. 优先使用稳定 `testTag`，不要依赖中文文案点击。
   - 首页设置入口是齿轮按钮，语义里不一定有“打开设置”。
   - 首页入口应使用 `settings-button`、`record-button`、`timeline-button`、`memory-button`。
   - 通用页面返回按钮使用 `page-back-button`，避免和 Result 页编辑态的“返回”按钮混淆。

2. 同名文本不要作为唯一选择器。
   - Result 页顶部返回和 NotePanel 编辑态返回都可能显示“返回”。
   - 如果测试需要点页面级返回，使用 `onNodeWithTag("page-back-button")`。
   - 如果一个页面存在多个相同标题或按钮文案，先给目标组件补 `testTag`。

3. 清空本地测试数据会改变导航状态。
   - Settings 的“清空本地测试数据”会重置 Room、DataStore，并把 App 导航回 Home。
   - 测试点击 `clear-local-data-button` 后，不要再点击“返回”。
   - 正确做法是等待首页默认内容或首页入口重新出现，例如等待 `timeline-button` 或默认标题。

4. 滚动页面上的按钮或文本，先 `performScrollTo()`。
   - `DiaryPage` 是纵向滚动容器。
   - Result 页编辑 NotePanel 后，当前滚动位置通常在页面下半部分。
   - 点击顶部返回前使用：

```kotlin
composeRule.onNodeWithTag("page-back-button").performScrollTo().performClick()
```

5. 编辑文本后处理软键盘和空闲状态。
   - `OutlinedTextField` 输入后，软键盘可能遮挡内容或影响后续点击。
   - 输入完成后使用：

```kotlin
closeSoftKeyboard()
composeRule.waitForIdle()
```

6. 区分“节点不存在”和“节点存在但不可见”。
   - `could not find any node`：选择器错误、页面没有切到预期状态，或测试还没等到数据。
   - `is not displayed`：节点在语义树里，但不在当前可见区域，常见于滚动页面。
   - 对长页面里的内容，先等待节点存在，再滚动到节点：

```kotlin
composeRule.waitUntil(timeoutMillis = 2_000) {
    composeRule.onAllNodesWithText(expectedText).fetchSemanticsNodes().isNotEmpty()
}
composeRule.onNodeWithText(expectedText).performScrollTo().assertIsDisplayed()
```

7. 不要用等待时间掩盖错误路径。
   - 如果 `waitUntil` 超时，先确认前一步点击是否真的触发了导航或状态变化。
   - 优先修 selector、滚动动作或导航副作用理解，而不是直接把 timeout 拉长。

8. 读报告时先找共同根因。
   - 多个测试同时失败在同一行或同一 selector，通常是测试选择器或共享前置状态错误。
   - `app/build/outputs/androidTest-results/connected/debug/*.xml` 比 HTML 报告更适合快速定位失败堆栈。
   - 先看失败测试名、失败行号、失败原因，再改测试或业务代码。

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
