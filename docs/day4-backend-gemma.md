# Day 4：FastAPI /generate-diary + Gemma 4 + 可调 Prompt

更新时间：2026-05-18

Day 4 目标是进入 tech-plan 第 4 阶段：把 Day 3 已经完成的 Record、图片选择、ROI、Result 图卡和本地落库链路，接到一个轻后端生成接口上。

```text
Android Record 提交文字 / 图片
  -> FastAPI POST /generate-diary
  -> 后端构造 prompt
  -> 调用 Google 托管 Gemma 4
  -> Pydantic 校验结构化 JSON
  -> Android 写入现有 Room / Result 链路
```

本阶段不实现语音识别 UI。Phase 5 剩余语音功能明天处理；今天只在请求和 prompt 中预留 `voiceText`。

## 1. 接口范围

新增后端接口：

```text
POST /generate-diary
```

请求 JSON：

```json
{
  "text": "今天真的有点累。",
  "voiceText": "",
  "dateId": "2026-05-18",
  "dateLabel": "5 月 18 日",
  "currentTitle": "今天看见的颜色",
  "isFirstRecordForDay": false,
  "username": "你",
  "estimatedDueDate": null,
  "image": {
    "mimeType": "image/jpeg",
    "dataBase64": "...",
    "dominantColor": "#D88B91"
  }
}
```

响应 JSON：

```json
{
  "titleSuggestion": "慢一点也认真",
  "diaryText": "今天的累不是失败，只是身体和心都在认真经过这一天。",
  "babyText": "妈妈辛苦啦，我在这里陪你慢慢来。",
  "safetyNote": "",
  "source": "gemma"
}
```

字段策略：

- `titleSuggestion`：Gemma 每次可以返回；Android 只有当天第一条 record 创建 entry 时使用，后续 append slide 不覆盖已有 `DiaryEntry.title`。
- `diaryText`：当前 record/slide 的正文，写入 note `selfText`。
- `babyText`：允许为空；只有合适时才出现，不强制每条都有。
- `safetyNote`：高风险孕期描述时返回克制提醒，不混入日记正文或宝宝说。
- `source`：开发调试字段，取值 `gemma | fallback | mock`，UI 不展示。
- 不返回 `caption` 和 `quote`；现有 `DiarySlide.caption` 由 Android 本地用 `diaryText` 短摘要或固定 fallback 填充。

## 2. Prompt Builder

后端新增独立 prompt 模块，避免 prompt 散落在 route 或模型 client 里：

```text
backend/
  app.py
  schemas.py
  prompt.py
  online_gemma_client.py
  generation_settings.py
  diary_fallbacks.py
  baby_reply_policy.py
```

`prompt.py` 提供纯函数：

```python
def build_generate_diary_prompt(request: GenerateDiaryRequest) -> str:
    ...
```

Prompt 采用分层结构，便于后续持续调试：

- **角色与产品语气**：私密孕期日记陪伴；先接住情绪，再整理记忆；温柔、克制、像日记，不像客服、鸡汤或母婴营销。
- **用户上下文**：`dateLabel`、`username`、`estimatedDueDate`、`currentTitle`、`isFirstRecordForDay`。
- **输入内容**：用户文本 `text`、语音转写 `voiceText`、图片输入说明和主色。
- **生成规则**：先理解用户语气；`diaryText` 是当前 record 正文，不是全天总结；`titleSuggestion` 短而适合作为当天标题。
- **宝宝说人设**：轻、短、依恋但不过度成熟；当用户提到累、疲惫、胎动或明显情绪波动时更容易出现；普通图片-only 可以为空；字数控制在约 12-36 个中文字符。
- **安全边界**：不诊断、不治疗、不建议药物；出血、剧烈疼痛、胎动明显减少等高风险描述写入 `safetyNote`，不让宝宝说承担医疗提醒。
- **输出契约**：只输出 JSON；必须包含 `titleSuggestion`、`diaryText`、`babyText`、`safetyNote`；空字段用空字符串。

Prompt 调试策略：

- `prompt.py` 定义 `PROMPT_VERSION = "day4_v1"`。
- 后端可以记录 prompt version 和生成 source，但不能记录完整用户原文或 base64 图片。
- 单测只保护关键约束，不 snapshot 整段 prompt，避免后续每次调 prompt 都要大改测试。

## 3. Gemma 调用

- 使用 `GEMINI_API_KEY` 调 Google 托管 Gemma。
- 默认模型为 `gemma-4-26b-a4b-it`，可用 `GEMINI_MODEL` 覆盖。
- 图片使用 inline base64 image part，不落盘。
- 模型输出经 Pydantic 校验。
- API key 缺失、模型失败、JSON 解析失败时返回 fallback，HTTP 仍为 200，保证 App 主流程不断。

## 4. Android 接入

- `submitRecord()` 优先调用后端；失败时回到 Day 3 本地 mock 生成。
- 图片上传使用用户选定 ROI 裁切后的压缩 JPEG，不上传整张原图。
- 增加 `INTERNET` 权限。
- Debug base URL：
  - emulator 默认 `http://10.0.2.2:8000`
  - 真机用 `-PbackendBaseUrl=http://<电脑局域网IP>:8000`
- 落库规则：
  - 当天第一条 record：使用 `titleSuggestion` 初始化 `DiaryEntry.title`
  - 当天后续 record：不覆盖已有 `DiaryEntry.title`
  - `diaryText -> DiaryNote.selfText`
  - `babyText -> DiaryNote.babyText`
  - `safetyNote` Day 4 暂不做完整 UI，只保留后续提示位

## 5. 测试计划

后端：

- `/health` 继续通过。
- `/generate-diary` text-only 返回合法 schema。
- image-only 返回合法 schema。
- `estimatedDueDate` 为空时 prompt 不出现奇怪占位。
- `isFirstRecordForDay=false` 时 prompt 明确 title 只是 suggestion。
- 用户表达“累”或“胎动”时，prompt 包含触发宝宝说的规则。
- 高风险文本返回非空 `safetyNote`，且不包含诊断、治疗、药物建议。
- 模型返回非法 JSON 或 `GEMINI_API_KEY` 缺失时 fallback。
- 日志不打印完整用户原文或 base64 图片。

Android：

- 后端成功：Result 展示后端 `diaryText` / `babyText`。
- 当天第一条 record：应用 `titleSuggestion`。
- 当天第二条 record：append slide，不覆盖已有 title。
- 后端失败或超时：fallback 到本地生成，不丢失文字和图片。
- 图片提交发送 ROI 裁切压缩图。
- Timeline / Memory 仍读取 entry title 和第一条 note selfText。

## 6. 本地调通后部署

- 本地调通后部署 FastAPI 到 Google Cloud Run。
- `GEMINI_API_KEY` 存 Secret Manager，Cloud Run 挂载为环境变量。
- `GEMINI_MODEL=gemma-4-26b-a4b-it`。
- Android staging/release 指向 Cloud Run HTTPS URL。
- Debug 可继续局域网 HTTP；release 不保留明文 HTTP。
- Demo 前固定 Cloud Run revision 和 Android version，并准备 fallback demo case。

## 7. 今日不做

- 不实现语音识别 UI，只预留 `voiceText`。
- 不修分享长图“宝宝说为空仍留白”问题，明天单独处理。
- 不把 `safetyNote` 做成完整 UI，只保证后端返回和本地链路不丢。

## 8. 2026-05-18 实施记录

- 已新增 `POST /generate-diary`，包含 request/response schema、prompt builder、Gemma client 和 fallback。
- Prompt 已集中到 `backend/prompt.py`，并定义 `PROMPT_VERSION = "day4_v1"`。
- 后端在 `GEMINI_API_KEY` 缺失或模型调用失败时返回 fallback，保证接口仍为 200。
- Android Record 提交已优先调用后端；失败时回到本地 mock 生成。
- Android 请求图片使用用户当前 ROI 裁切后的 JPEG base64。
- Android 新增 `INTERNET` 权限，并通过 `backendBaseUrl` Gradle property 支持真机局域网调试。
- 本轮已通过 `python -m pytest`、`.\gradlew.bat :app:assembleDebug`、`.\gradlew.bat :app:testDebugUnitTest`。
