# You & Me Diary 技术方案与开发计划

更新时间：2026-05-16

目标设备：vivo X100

目标形态：Android App

目标节奏：下班后开发，1-2 周完成可演示 MVP，并预留后续迭代优化时间。

## 1. MVP 目标

第一版 MVP 不追求完整生产级能力，目标是做出一个能在真机上跑通的核心闭环：

```text
记录今天
  -> AI 生成日记内容
  -> 生成日记图
  -> 自动进入时间线
  -> 收藏进入纪念册
  -> 生成可下载分享长图
```

MVP 必须完成：

- Android 原生 App
- 本地记录文字
- 选择或拍摄图片
- 调用 Gemma 生成结构化日记内容
- 本地生成日记图卡
- 时间线
- 纪念册
- 设置页
- 分享长图导出

MVP 暂缓：

- 账号系统
- 云同步
- 医疗知识库
- 复杂孕周推荐
- 真正多轮聊天
- 训练或微调模型
- 复杂后端
- 端侧模型下载与部署

## 1.1 当前架构决策

MVP 采用 `云端 Gemma 推理 + Android 本地存储/渲染`。

原因：

- 1-2 周内更容易做出稳定 demo
- 不需要每个用户下载 Gemma 模型
- 服务端可以使用更大的 Gemma 模型，中文表达和图文理解质量更好
- Android 端不用处理模型格式、内存、推理速度、发热、电量等问题
- 更适合把时间投入到产品体验、日记图、时间线、纪念册和分享长图上

产品叙事：

```text
当前 MVP 使用云端 Gemma 获得更强的多模态生成质量。
App 侧默认本地保存日记、图片、时间线和纪念册。
服务端只做临时推理，不长期保存用户原始文本和图片。
后续可提供端侧 Gemma Lite/Offline 模式，作为隐私和离线增强。
```

## 2. 技术选型

### 2.1 Android

建议使用：

- Kotlin
- Jetpack Compose
- Material 3 基础组件
- Room
- DataStore
- CameraX 或系统 Photo Picker
- Android ShareSheet

原因：

- UI 迭代快
- 适合做当前这种卡片、图册、设置页、日历页
- Kotlin + Compose 对个人开发更省时间
- MVP 不需要跨平台复杂度

### 2.2 Gemma / Cloud AI

推荐优先路线：

- 后端部署 Gemma 推理服务
- MVP 优先使用更大的 Gemma 文本/多模态模型
- Android App 通过 HTTPS 调用后端接口
- App 本地完成数据保存、图卡渲染和分享长图导出

原因：

- 云端可以使用更大模型
- 避免移动端模型下载
- 避免端侧推理兼容性和性能风险
- 更容易快速迭代 prompt
- 更容易保证 JSON 输出稳定性

后续增强：

- MediaPipe LLM Inference API
- Gemma 3n E2B/E4B 端侧模式
- Android 本地隐私/离线生成

参考：

- https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
- https://ai.google.dev/edge/litert/android
- https://ai.google.dev/gemma/docs

## 3. MVP 架构

建议采用轻后端 + 本地优先 App 架构。

```text
Android App
  ├─ UI 层
  │   ├─ 首页
  │   ├─ 记录页
  │   ├─ 结果页
  │   ├─ 时间线
  │   ├─ 纪念册
  │   └─ 设置页
  │
  ├─ 领域层
  │   ├─ CreateEntryUseCase
  │   ├─ GenerateDiaryUseCase
  │   ├─ RenderDiaryCardUseCase
  │   ├─ FavoritePageUseCase
  │   └─ ExportShareImageUseCase
  │
  ├─ 数据层
  │   ├─ Room 数据库
  │   ├─ 本地文件存储
  │   ├─ DataStore 设置
  │   └─ Prompt 模板
  │
  ├─ AI 层
  │   ├─ DiaryApiClient
  │   ├─ JsonParser
  │   ├─ 重试 / 兜底
  │   └─ SafetyGuard
  │
  └─ 媒体层
      ├─ ImagePicker
      ├─ AudioRecorder / 语音输入
      ├─ PaletteExtractor
      └─ CardRenderer

后端
  ├─ API 层
  │   ├─ POST /generate-diary
  │   └─ GET /health
  │
  ├─ AI 层
  │   ├─ GemmaClient
  │   ├─ PromptBuilder
  │   ├─ ResponseParser
  │   └─ SafetyPostProcessor
  │
  └─ 隐私层
      ├─ 临时上传存储
      ├─ 请求大小限制
      ├─ 不记录原始日记
      └─ 自动删除临时文件
```

## 4. 前后端分层

### 4.1 MVP 使用轻后端

MVP 建议做一个非常薄的后端，只负责 Gemma 推理。

后端职责：

- 接收用户文本和图片
- 构造 prompt
- 调用 Gemma
- 返回结构化 JSON
- 删除临时图片
- 不保存原始日记
- 不做账号系统
- 不做云同步

Android App 职责：

- UI 展示
- 本地数据库
- 本地图片保存
- 时间线
- 纪念册
- 日记图生成
- 分享长图导出

### 4.2 隐私边界

服务端不保存：

- 用户原始日记
- 孕期照片
- 身体状态记录
- 分享图

服务端可以保存：

- 匿名请求耗时
- 模型错误码
- JSON 解析失败率
- 不含原文的 prompt 版本号

### 4.3 后端技术选型

建议优先：

- FastAPI
- Python
- Pydantic
- Uvicorn
- Docker

原因：

- 写起来快
- 多模态文件上传处理简单
- Pydantic 很适合校验模型输出 JSON
- 个人开发更轻

可选：

- Ktor
- Node.js / Express

如果你想和 Android/Kotlin 技术栈统一，可以选 Ktor。但从 1-2 周 MVP 速度看，FastAPI 更省心。

## 5. Gemma 部署方案

### 5.1 MVP 云端部署

MVP 优先将 Gemma 放在服务端。

```text
Android App
  -> POST /generate-diary
  -> 后端调用 Gemma
  -> 后端返回结构化 JSON
  -> App 在本地渲染日记卡
```

### 5.2 API 设计

```http
POST /generate-diary
Content-Type: multipart/form-data
```

Request：

```text
text: String
date: String
username: String
due_date: String?
images: File[]
locale: zh-CN
```

Response：

```json
{
  "date_title": "4 月 26 日 · 一次小小胎动",
  "mood_emoji": "✨",
  "mood_color": "#D58B93",
  "comfort_text": "...",
  "slides": [
    {
      "title": "午餐之后",
      "quote": "...",
      "caption": "...",
      "source_image_index": 0,
      "notes": [
        {
          "label": "恢复感",
          "self_text": "...",
          "baby_text": "...",
          "x": 0.62,
          "y": 0.35
        }
      ]
    }
  ],
  "timeline_summary": "...",
  "safety_note": null
}
```

### 5.3 模型选择

MVP 推荐：

- 云端 Gemma 大模型
- 优先选择支持图片输入的 Gemma 多模态模型
- 如果云端多模态暂时不稳定，先用图片元信息 + 文本生成

后续端侧增强：

- Gemma 3 1B 4-bit 文本模式
- Gemma 3n E2B/E4B 多模态模式
- MediaPipe LLM Inference API

### 5.4 端侧模式保留为后续

端侧 Gemma 不作为 MVP 第一优先级。

后续做法：

- 使用 MediaPipe LLM Inference API
- 开发期通过 `adb push` 把模型放入测试机
- 正式版本运行时下载模型
- 提供 `隐私模式 / 离线模式`

端侧模式适合成为比赛加分项，而不是第一周开发主线。

### 5.5 推理方式

MVP 使用单次生成，不做复杂多轮会话。

每次记录生成一次结构化结果：

```json
{
  "date_title": "4 月 26 日 · 一次小小胎动",
  "mood_emoji": "✨",
  "mood_color": "#D58B93",
  "comfort_text": "...",
  "slides": [
    {
      "title": "午餐之后",
      "quote": "...",
      "caption": "...",
      "notes": [
        {
          "label": "恢复感",
          "self_text": "...",
          "baby_text": "..."
        }
      ]
    }
  ],
  "timeline_summary": "...",
  "safety_note": null
}
```

### 5.6 后端最小实现

FastAPI 结构建议：

```text
backend/
  ├─ app.py
  ├─ schemas.py
  ├─ prompt.py
  ├─ gemma_client.py
  ├─ safety.py
  └─ requirements.txt
```

核心接口：

```python
@app.post("/generate-diary")
async def generate_diary(
    text: str = Form(...),
    date: str = Form(...),
    username: str = Form("妈妈"),
    due_date: str | None = Form(None),
    images: list[UploadFile] = File(default=[])
):
    ...
```

开发期可以先让后端返回 mock JSON，再接真实 Gemma。

## 6. 图片与日记图生成

MVP 不建议一开始做真正的图像生成。

建议用本地渲染：

- 用户图片作为底图
- 提取主色
- 添加渐变遮罩
- 叠加 AI 生成文案
- 导出 Bitmap

实现方式：

- Compose UI 展示图卡
- Android Canvas 导出当前 slide 的分享长图
- 图片主色可先用简单算法：缩小图片后取 dominant colors
- 后续再优化为 Palette 或 K-means

这样更稳定，也更适合 1-2 周 MVP。

## 7. 语音输入策略

MVP 推荐：

- 第一版用系统语音输入或 Android SpeechRecognizer 转文字
- 同时保留纯文字输入

原因：

- 音频直接进 Gemma 3n 虽然官方支持，但 Android 上调通音频格式、模型和性能会增加风险
- MVP 的产品价值主要来自“低负担表达”和“结果生成”
- 先把语音变成文字，再交给 Gemma 生成，速度更可控

比赛增强：

- 尝试 Gemma 3n E2B 的 audio input
- 用音频 + 文本生成更自然的情绪理解

## 8. 本地数据模型

### 8.1 日记记录 Entry

```kotlin
data class DiaryEntry(
    val id: String,
    val date: LocalDate,
    val rawText: String,
    val audioPath: String?,
    val createdAt: Instant,
    val moodEmoji: String,
    val moodColor: String,
    val timelineSummary: String,
    val comfortText: String
)
```

### 8.2 媒体 Media

```kotlin
data class EntryMedia(
    val id: String,
    val entryId: String,
    val localPath: String,
    val type: MediaType,
    val dominantColors: List<String>
)
```

### 8.3 图页 Slide

```kotlin
data class DiarySlide(
    val id: String,
    val entryId: String,
    val mediaId: String?,
    val title: String,
    val quote: String,
    val caption: String,
    val isFavorite: Boolean
)
```

### 8.4 注释 Note

```kotlin
data class SlideNote(
    val id: String,
    val slideId: String,
    val label: String,
    val selfText: String,
    val babyText: String,
    val editedSelfText: String?,
    val editedBabyText: String?,
    val x: Float,
    val y: Float
)
```

### 8.5 设置 Settings

```kotlin
data class UserSettings(
    val username: String,
    val dueDate: LocalDate?,
    val colorTheme: String
)
```

## 9. Prompt 设计

MVP 使用固定 prompt，要求模型输出 JSON。

输入：

- 用户原始文本
- 语音转写文本
- 图片基本信息
- 日期
- 用户名
- 预产期/孕周

输出要求：

- 不诊断
- 不给医疗结论
- 输出温柔但不过度煽情
- 每张 slide 生成 quote/caption/notes
- notes 同时包含 self_text 和 baby_text

安全补充：

- 若出现异常身体描述，生成 `safety_note`
- 例如明显加重、出血、剧烈疼痛、胎动异常等，应提示联系医生

## 10. 开发计划

### 第 0 阶段：准备，0.5 天

目标：

- 创建 Android 项目
- 创建轻后端项目
- 确认可在 vivo X100 上安装运行
- 确认 App 能访问后端 `/health`
- 准备 3-5 条 mock 数据

输出：

- 空 App
- 空后端
- 页面导航骨架
- 本地 mock 数据

### 第 1 阶段：UI 主流程，2-3 天

目标：

- 首页
- 设置页
- 记录页
- 生成中页
- 结果页
- 时间线
- 纪念册

优先做静态与 mock 数据，不接模型。

输出：

- 可完整点击的 Android 原生 UI
- 页面结构与当前 HTML 原型基本一致

### 第 2 阶段：本地存储，1-2 天

目标：

- Room 表结构
- DataStore 设置
- 图片保存到本地
- 收藏逻辑
- 时间线从数据库读取
- 纪念册从收藏读取

输出：

- 创建记录
- 自动进入时间线
- 收藏图页进入纪念册

### 第 3 阶段：Record + 图卡渲染，1-2 天

目标：

- 按 `docs/prototypes/record-redesign.html` 重写 Record 页
- 接入单图 Photo Picker
- 同一天多次提交聚合到一个 `DiaryEntry`，每次提交追加一张 `DiarySlide`
- 根据用户图片生成日记图，视觉参考 `docs/prototypes/color-walk.jpg`
- 图片主色提取
- 文案叠加
- 当前 slide 分享长图预览和导出

输出：

- 结果页图册可左右滑
- 同一天多次提交会在 Result 中显示为多张图卡
- 当前 slide 长图可保存到相册或通过系统分享

### 第 4 阶段：后端 API 与 Gemma 推理，2-3 天

目标：

- 实现 FastAPI `/generate-diary`
- 先返回 mock JSON
- 接入云端 Gemma 推理
- 生成结构化 JSON
- Pydantic 校验响应
- 解析失败时 fallback 到安全模板
- 临时图片推理后删除
- 不记录用户原文和图片

输出：

- 用户输入真实文本后，生成真实日记内容
- Android App 可通过 HTTPS 获取 Gemma 结果

### 第 5 阶段：语音与图片输入，1-2 天

目标：

- 接入系统图片选择器
- 接入语音转文字
- 将语音转写和图片信息放入 prompt

输出：

- 文字 + 语音 + 图片的完整输入体验

### 第 6 阶段：打磨与录 demo，2-3 天

目标：

- 优化文案
- 优化配色
- 优化加载状态
- 真机性能测试
- 录制比赛 demo

输出：

- 1 个完整 demo case
- 1 个备用 demo case
- 录屏脚本

## 11. 1-2 周排期建议

如果下班后每天 1.5-2.5 小时，建议这样排：

### 第 1 周

Day 1：

- Android 项目搭建
- FastAPI 项目搭建
- 页面导航骨架
- mock 数据

Day 2：

- Home / Settings / Record

Day 3：

- 结果页图册与注释交互

Day 4：

- 时间线日历
- 纪念册收藏

Day 5：

- Room + DataStore

Weekend：

- 图卡渲染
- 分享长图导出
- 整体 UI 打磨
- 后端 `/generate-diary` mock 接口

### 第 2 周

Day 1：

- 接入云端 Gemma
- 后端 prompt builder

Day 2：

- Prompt 输出 JSON
- JSON 解析和 fallback
- Pydantic schema 校验

Day 3：

- 图片输入
- 语音转文字
- Android 调用真实后端

Day 4：

- 真机网络与生成耗时测试
- Prompt 文案优化
- 隐私日志检查

Day 5：

- Bug 修复
- Demo 数据固化

Weekend：

- 多模态效果优化
- 录屏
- 比赛提交材料整理

## 12. 风险与降级方案

### 风险 1：云端 Gemma 多模态效果或接口不稳定

降级：

- 先用文本生成
- 图片先用本地算法提取颜色和布局
- 后端返回稳定 mock JSON 做 demo fallback
- demo 中说明下一阶段增强图文多模态理解

### 风险 2：网络延迟或后端生成速度慢

降级：

- 限制输入长度
- 限制输出 JSON 长度
- 每次只生成 2-3 张 slide
- 使用 loading 过渡
- 缓存结果
- 支持手动重试

### 风险 3：模型输出 JSON 不稳定

降级：

- 使用严格 prompt
- 使用 Pydantic 校验
- 用 JSON repair 做一次修复
- 失败时走模板生成

### 风险 4：医疗安全表达不稳

降级：

- 在 App 层加 SafetyGuard
- 在后端加 SafetyPostProcessor
- 命中高风险词时固定追加就医提醒
- 不允许模型输出诊断结论

### 风险 5：隐私顾虑影响产品信任

降级：

- 明确展示“云端生成、本地保存”
- 服务端不保存原文和图片
- 临时文件推理后删除
- 后续提供端侧隐私模式

## 13. MVP 成功标准

MVP 做成后，应能完成以下演示：

1. 用户打开 App，进入记录页
2. 输入一段孕期状态文字，附一张图片
3. App 调用 Gemma 生成日记结构
4. 结果页展示 2-3 张日记图
5. 点击图片注释，切换 `我 / 宝`
6. 编辑一段文本
7. 收藏当前图页
8. 在纪念册看到收藏
9. 在时间线点击日期回到结果页
10. 生成并下载分享长图

## 14. 后续增强方向

比赛增强优先级：

- Gemma 3n E2B 图文多模态
- Gemma 3n 音频输入
- 端侧 Gemma 隐私/离线模式
- 孕周上下文提示
- 伴侣共情卡
- 导出完整孕期纪念册
- 端侧 prompt 模板切换
- 性能 benchmark 截图或日志
