# Day 6：语音输入准备与 Android 生成 Gateway

更新时间：2026-06-03

Day 6 的目标是进入语音最小可用版之前，先把 Android 生成链路收窄和统一。这样后续接入语音转写时，`DiaryAppViewModel` 只需要设置 `voiceText`、`inputSource` 和 `diaryTextMode`，不需要再分别维护 online / offline 两套 client、两套 request 和两套图片预处理逻辑。

本阶段先做生成链路重构，不实现语音 UI，也不改后端 API、Cloud Run 或 Room schema。

## 1. 背景

重构前：

- `DiaryAppViewModel` 同时持有 `RemoteGemmaClient` 和 `LocalGemmaClient`。
- ViewModel 内部按 `generationMode` 分支构造 `GenerateDiaryRemoteRequest` 或 `GenerateDiaryLocalRequest`。
- remote 图片 base64、local 临时裁剪图、ROI 裁切、384px JPEG 压缩都混在 ViewModel 中。
- 后续接语音时，ViewModel 需要同时改 online / offline 两条路径，扩展风险高。

重构目标：

- ViewModel 只描述“这次生成需要什么”。
- online / offline 选择交给统一 gateway。
- remote / local 适配细节下沉到各自 adapter。
- 图片预处理集中到一个 helper。
- 为语音输入、端侧模型和云端模型切换留出稳定接口。

## 2. 当前链路

```text
DiaryAppViewModel
  -> DiaryGenerationRequest
  -> DiaryGenerationGateway
      -> RemoteDiaryGenerator -> RemoteGemmaClient -> Cloud Run /generate-diary
      -> LocalDiaryGenerator  -> LocalGemmaClient  -> LiteRT-LM local Gemma
  -> GeneratedDiaryDraft?
  -> DiaryRepository.createOrAppendTodayEntry(...)
```

## 3. 新增 Android 文件

```text
app/src/main/java/com/youandme/diary/data/generation/
  DiaryGenerator.kt
  DiaryGenerationRequest.kt
  DiaryGenerationGateway.kt
  RemoteDiaryGenerator.kt
  LocalDiaryGenerator.kt
  DiaryGenerationImageProcessor.kt
```

关键职责：

- `DiaryGenerator`：统一生成接口，返回 `GeneratedDiaryDraft?`。
- `DiaryGenerationRequest`：统一请求模型，包含文本、预留语音文本、输入来源、正文模式、日期、用户设置、图片路径、ROI、主色和生成模式。
- `DiaryGenerationGateway`：根据 `GenerationModes.Online / Offline` 分发到 remote 或 local，并提供 local warm-up 入口。
- `RemoteDiaryGenerator`：包装 `RemoteGemmaClient`，把统一 request 转成 `GenerateDiaryRemoteRequest`，并准备 remote 图片输入。
- `LocalDiaryGenerator`：包装 `LocalGemmaClient`，把统一 request 转成 `GenerateDiaryLocalRequest`，并负责 local 临时裁剪图清理。
- `DiaryGenerationImageProcessor`：集中处理主色估算、ROI 裁切、384px JPEG 压缩、remote base64 和 local model image。

## 4. ViewModel 边界

`DiaryAppViewModel` 现在保留：

- 收集 UI 状态。
- 复制用户选择的原图到本地。
- 计算日期、`currentTitle`、当天第一条状态。
- 决定 `inputSource`：
  - 纯图片：`imageOnly`
  - 当前文本输入：`typed`
  - 未来语音输入：`voice` 或 `mixed`
- 决定 `diaryTextMode`：
  - `preserve`：普通短文本，尽量保留用户原文。
  - `polish`：长文本或语音转写文本，只整理断句和轻微错字。
  - `generate`：纯图片输入，根据图片生成正文。
- 调用 `DiaryGenerationGateway.generate(...)`。
- 把 `GeneratedDiaryDraft?` 交给 `DiaryRepository` 落库并导航。

ViewModel 不再直接依赖：

- `RemoteGemmaClient`
- `LocalGemmaClient`
- `GenerateDiaryRemoteRequest`
- `GenerateDiaryLocalRequest`
- `DiaryRemoteImage`

## 5. 图片处理策略

当前所有“有图”请求都会传图，不再走“只传主色”的分支。

remote：

- 根据用户选定 ROI 裁切图片。
- 压缩为 384px JPEG，有损编码。
- 转为 base64，作为 `DiaryRemoteImage` 上传到 Cloud Run。

local：

- 根据用户选定 ROI 裁切图片。
- 压缩为 384px JPEG，有损编码。
- 写入 cache 下的 local 临时图片。
- local Gemma 推理结束后清理临时图片。

异常处理：

- 主色估算、图片压缩或临时图写入失败时记录 `DiaryGeneration` 日志并返回 `null`。
- 生成失败仍由 repository 保持现有 fallback，主流程不断。

## 6. 与语音功能的关系

语音最小可用版计划在 Record 页发生：

```text
用户按住说话
  -> Android 录音 / SpeechRecognizer 转文字
  -> 转写结果填入 voiceText
  -> inputSource = voice 或 mixed
  -> diaryTextMode = polish
  -> 用户点击提交
  -> 统一 DiaryGenerationGateway
```

因此语音接入时，主要新增 UI 状态、录音/转写流程和 request 字段填充，不需要再改 online / offline client 分支。

## 7. 实施记录

- 新增 `data/generation` 统一生成层。
- 移除 `typealias DiaryGenerationClient = RemoteGemmaClient`，避免“生成 client”等同于 remote client。
- `DiaryAppViewModel` 不再构造 remote/local 专用 request。
- 图片预处理从 ViewModel 移到 `DiaryGenerationImageProcessor`。
- online 模式仍走 Cloud Run，offline 模式仍走 LiteRT-LM local Gemma。
- online token 不提交到仓库；具体构建、安装和 token 注入方式见 `docs/build.md`。

## 8. 验证

已通过：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

真机验证：

- offline 图片 + 文本生成正常。
- online 重新注入 token 后生成正常。
- debug 包安装说明见 `docs/build.md`。
