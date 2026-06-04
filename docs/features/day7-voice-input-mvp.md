# Day 7：语音输入 MVP

更新时间：2026-06-04

Day 7 目标是实现语音输入最小可用版。当前 Android 生成链路已经在 Day 6 重构为 `DiaryGenerationGateway`，详见 `docs/features/day6-voice-and-generation-gateway.md`。因此本阶段只新增 Record 页语音交互、云端语音转写、ViewModel 语音状态和 request 字段填充；online / offline 分支仍收敛在 `DiaryGenerationGateway` 和对应 adapter 内。

vivo X100 / V2548A 真机日志显示设备没有暴露 Android 标准 `SpeechRecognizer` 服务，因此本阶段从系统语音识别改为 App 自己录制短音频，再交给 `DiaryGenerationGateway` 按 generation mode 分流转文字。Room schema 不变；语音只在 Record 提交前转成文字，再走现有 `DiaryGenerationGateway`。

## 1. 当前结构复盘

当前生成链路：

```text
DiaryAppViewModel
  -> DiaryGenerationRequest
  -> DiaryGenerationGateway
      -> RemoteDiaryGenerator -> RemoteGemmaClient -> Cloud Run /generate-diary
      -> LocalDiaryGenerator  -> LocalGemmaClient  -> LiteRT-LM local Gemma
  -> GeneratedDiaryDraft?
  -> DiaryRepository.createOrAppendTodayEntry(...)
```

当前代码状态：

- `RecordScreen` 已接入麦克风 / 键盘切换、语音横向按钮、录音计时和转录状态。
- `AndroidManifest.xml` 已增加 `RECORD_AUDIO`。
- Record 页面已接入运行时录音权限请求。
- MVP 参考 Google AI Edge Gallery，使用 `AudioRecord` 采集 16k mono PCM 16-bit bytes，不保存音频路径。
- Online 模式把 PCM bytes 包成 WAV 后上传到 `/transcribe-voice`；该接口使用 `X-App-Token` 保护，调用云端模型做转录；失败时返回空 transcript，App 展示轻量错误。
- Offline 模式也把 PCM bytes 包成 WAV 后传给 LiteRT-LM `Content.AudioBytes` 做端侧转录，`audioBackend` 使用 CPU。
- `DiaryGenerationRequest` 已包含 `voiceText`、`inputSource`、`diaryTextMode`。
- remote/local adapter 已经会透传 `voiceText`。
- `diaryTextModeFor(...)` 已经定义：有 `voiceText` 时使用 `polish`。

## 2. 交互方案

参考 `docs/prototypes/record-redesign.html`：

- 点击麦克风按钮进入语音模式。
- 语音模式下，文本输入框位置变成横向按钮，文案为 `按住 说话`。
- 麦克风按钮切换为键盘 icon。
- 再次点击键盘 icon 时退出语音模式，并清空当前输入。
- 用户按住语音按钮开始识别，松开停止识别。
- 最长记录 60s；到 60s 自动停止。
- 停止后进入转录状态，文案为 `正在转录...`。
- 转录完成后切回文本输入状态，文本框显示转写文本，用户可以继续编辑。
- 图片选择、ROI 调整和提交按钮逻辑保持现状。

UI 约束：

- 文本输入框和语音横向按钮高度、内边距和基线尽量保持一致。
- `今天想说...` 与转写后的文本在同一个输入容器里呈现，避免位置跳动明显。
- `正在转录...` 使用当前主题的 primary/accent 柔和变体，不能跳出整体风格。
- 按住按钮要有按下态，但不要扩大整体 composer 高度。

## 3. 实现边界

MVP 使用 Android `AudioRecord` 录制 PCM bytes，不保存音频路径。

需要新增：

- 后端新增 `POST /transcribe-voice`，仅用于 Online 转录：
  - 请求体包含 `mimeType`、`dataBase64`、`locale`。
  - 请求头必须带 `X-App-Token`。
  - 使用 `GEMINI_API_KEY` 调用模型转写音频。
  - 返回 `transcript`、`source`、`debugErrorType`、`debugErrorMessage`。
- Android 新增 `VoiceTranscriptionClient`：
  - Online 模式把 PCM bytes 包成 WAV，base64 后上传到 `/transcribe-voice`。
  - 只返回 transcript；失败返回 `null`。
- Android 在 Offline 模式调用 `DiaryGenerationGateway -> LocalDiaryGenerator -> LocalGemmaClient.transcribeAudio(...)`：
  - 先把 `AudioRecord` 采集到的 PCM bytes 包成 WAV，再使用 `Content.AudioBytes(wavBytes)` 和本地 Gemma 做端侧转写。
  - 如果当前模型不支持音频，会显示 `端侧语音转写暂不可用`，并通过 `LocalGemma`/`DiaryGeneration` 日志定位。
- `AndroidManifest.xml` 保留 `RECORD_AUDIO` 权限。
- Record 页面触发运行时录音权限请求，并用 `AudioRecord` 控制按住录音。
- 新增语音状态：
  - `isVoiceMode`
  - `isVoiceRecording`
  - `isVoiceTranscribing`
  - `voiceElapsedSeconds`
  - `recordVoiceText`
- 新增 ViewModel 操作：
  - 进入/退出语音模式。
  - 开始/停止录音状态。
  - 设置转写文本。
  - 转写失败时回到语音输入待机态，保留用户原文本或显示轻量错误状态。

两次模型相关调用：

- 第一次发生在 Record 阶段：录音结束后由 `DiaryGenerationGateway` 按 generation mode 调用 online 或 offline 转录，只把音频变成文本。
- 第二次发生在提交阶段：用户确认文本和图片后，按现有 online/offline 生成模式调用 `/generate-diary` 或端侧 local Gemma。
- 转写后的文本会回填到输入框，用户仍然可以编辑；不会在转写完成时自动生成日记。
- MVP 不保存临时音频文件；录音只在内存中保留为 PCM bytes，并在 online/offline 转录前包成 WAV payload。

## 4. 提交语义

生成 request 字段规则：

- 只有转写文本，没有手动输入文本：
  - `text = ""`
  - `voiceText = recordVoiceText`
  - `inputSource = voice`
  - `diaryTextMode = polish`
- 手动输入文本和转写文本同时存在：
  - `text = recordText`
  - `voiceText = recordVoiceText`
  - `inputSource = mixed`
  - `diaryTextMode = polish`
- 只有手动输入短文本：
  - `inputSource = typed`
  - `diaryTextMode = preserve`
- 只有图片：
  - `inputSource = imageOnly`
  - `diaryTextMode = generate`
- 有图片时仍传图；语音不会改变图片 ROI / JPEG / base64 处理逻辑。

落库策略：

- `rawText` 优先使用用户能看到的最终文字。
- 语音 MVP 暂不新增 Room 字段，所以转写文本会进入现有 note 文本链路。
- 为避免用户以为音频被保存，MVP 不展示音频回放，也不保存音频路径。

## 5. 实施记录

已完成：

- `DiaryAppViewModel` 新增语音状态：
  - `isRecordVoiceMode`
  - `isRecordVoiceRecording`
  - `isRecordVoiceTranscribing`
  - `recordVoiceElapsedSeconds`
  - `recordVoiceText`
  - `recordVoiceError`
- `submitRecord()` 已支持 voice-only 和 mixed：
  - voice-only：`inputSource=voice`、`diaryTextMode=polish`
  - text + voice：`inputSource=mixed`、`diaryTextMode=polish`
  - image-only：仍保持 `inputSource=imageOnly`、`diaryTextMode=generate`
- `RecordScreen` 已实现：
  - 麦克风 / 键盘切换。
  - 横向 `按住 说话` 按钮。
  - `正在记录 00:xx` 计时。
  - `正在转录...` 状态。
  - 60s 自动停止。
- `AndroidManifest.xml` 已增加 `RECORD_AUDIO`。
- 后端已新增 `/transcribe-voice` schema、endpoint、Gemini 转写 client 和基本鉴权测试。
- Android 已新增 `VoiceTranscriptionClient`，Record 页已从 `SpeechRecognizer` 改为 `AudioRecord` PCM 录音。
- Offline 模式已新增端侧 `transcribeAudio(...)`，参考 Google AI Edge Gallery 使用 `Content.AudioBytes`；当前实现会先把 PCM 包成 WAV payload，local prompt 已修复为同时包含 `用户手写`、`语音转写` 和 `合并输入`。
- 图卡字段策略已修正：`cardSummary` 是最多 8 个中文字符的文字短句，`cardEmoji` 是单个情绪 emoji，UI 显示为 `cardSummary + cardEmoji`。
- 转写失败、无权限、网络失败等场景会回到轻量错误提示，不阻断图片/文字提交。

## 6. 后续真机回归

需要在 vivo X100 / V2548A 上确认：

- 首次点击语音按钮会弹录音权限。
- 授权后按住说话可以进入计时。
- 松开后进入转录状态并回填文本。
- 点击键盘 icon 退出语音模式并清空输入。
- voice-only、text + voice、voice + image、image-only 均能正常生成。
- Online 模式 Cloud Run 最新 revision 包含 `/transcribe-voice`。
- Offline 模式本地 WAV `Content.AudioBytes` 转写需要继续用 vivo X100 真机确认。

## 7. 验证

本轮改动后需要重新验证：

```powershell
python -m pytest backend
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

待真机确认：

- 真机授权录音权限。
- Record 点击麦克风进入语音模式，再点键盘退出并清空输入。
- 按住说话，松开后按 generation mode 转录音频，进入转录状态并回填文本。
- voice-only 提交进入 `inputSource=voice`、`diaryTextMode=polish`。
- text + voice 提交进入 `inputSource=mixed`、`diaryTextMode=polish`。
- 纯图片仍为 `inputSource=imageOnly`、`diaryTextMode=generate`。
