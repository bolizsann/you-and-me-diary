# Day 3：Record + 图卡渲染 + Media 数据链路

更新时间：2026-05-17

Day 3 的目标是把 Phase 3 从“mock 图卡”推进到真实输入闭环：

```text
Record 选择图片 / 输入文字
  -> 生成或追加今天的一张 slide
  -> Result 展示当天多张图卡
  -> 当前 slide 长图预览与导出
```

本阶段不接 Gemma、不接语音识别、不做 AI 生图。后续 Gemma 只替换 mock 文案生成，不改变本地保存、Result 渲染、Timeline、Memory Book 和分享导出的主链路。

## 1. 产品与 UI 范围

### Record 页

Record 页按 `docs/prototypes/record-redesign.html` 重写：

- 中部是正方形图片上传框。
- 默认只保留短句：`What you see can hold what you feel.`
- 图片上传后显示图片预览，底部只保留“更换图片”。
- 底部是 agent/chat 式输入条：文本输入、mic 占位 icon、右侧 `↑` 提交。
- 去掉大段说明和大号生成按钮。
- 提交条件：图片或文字至少有一个。
- Phase 3 只支持一次提交一张图片。

### Result 页

Result 页保留当前交互语义，填充真实功能：

- 同一天所有提交显示为同一个 Result 图册里的多张 slides。
- 横滑、页码和 dots 对应当天 slide 数。
- 收藏仍是收藏当天整条内容，而不是收藏单张 slide。
- 编辑正文继续持久化。
- 分享长图先只导出当前 slide，不做当天全部 slides 拼接。

### 图卡视觉

参考 `docs/prototypes/color-walk.jpg`，图卡采用克制的两段式：

```text
上半部分：图片主色生成色块 / 轻渐变，放短句或时间感文案
下半部分：用户图片 center-crop 展示
```

实现原则：

- 有图片时，下半部分展示图片，叠加轻遮罩保证视觉柔和。
- 上半部分使用 dominant color 与 slide 渐变色，避免文字压在复杂照片上。
- 无图片时，继续使用当前抽象渐变兜底。
- 不引入字体文件，先用系统字体模拟轻手写/日记感。

## 2. 数据字段策略

当前立即做：

- 新增 domain `EntryMedia`。
- `DiaryEntry` 增加 `media: List<EntryMedia> = emptyList()`。
- `DiarySlide` 增加 `mediaId: String? = null`。
- `DiaryEntryWithSlides` 增加 `media: List<EntryMediaEntity>` relation。
- mapper 读取 `EntryMediaEntity -> EntryMedia`，并把 `DiarySlideEntity.mediaId` 映射到 domain。
- mapper 写入时使用 `slide.mediaId`，不再固定为 `null`。
- repository 新增 `createOrAppendTodayEntry(text, localImagePath, dominantColor)`：
  - 今天无 entry 时创建当天 entry、第一张 slide 和可选 media。
  - 今天已有 entry 时追加 slide 和可选 media。
- 继续使用现有表字段：`entry_media` 与 `diary_slides.mediaId`。
- 不升 Room version，不写 migration。

当前暂不做：

- 不新增 `updatedAt`。
- 不新增 `DiarySlideEntity.createdAt`。
- 不新增 `slide.diaryText` 或 `slide.sourceText`。
- 不新增多图关联表。
- 不整理或删除 `diary_notes`，继续作为正文编辑兼容层。

后续 Gemma 阶段再评估：

- 正式 slide-level 文案字段，例如 `diaryText`、`generatedAt`、`promptVersion`。
- 每次提交准确时间字段，例如 `DiarySlideEntity.createdAt`。
- Gemma 返回更丰富结构后的 v2 schema 和 Room migration。

## 3. 同一天多次提交

Day 3 的数据语义：

```text
同一天一个 DiaryEntry
一次提交追加一张 DiarySlide
```

例子：

```text
5 月 16 日
  slide 1：10am 的图片 / 心情
  slide 2：12pm 的图片 / 心情
  slide 3：晚上的图片 / 心情
```

Timeline 仍然一天一个入口。Memory Book 收藏后仍然收藏当天整条内容。

## 4. 长图分享

Phase 3 只做当前 slide 的分享图：

```text
日期
当前图卡
当前 slide 标题 / 正文
宝宝说
You & Me Diary 署名
```

导出方式：

- 使用 Android Canvas 生成 PNG。
- 先保存到 MediaStore 的 Pictures/YouAndMeDiary。
- 再通过系统 ShareSheet 分享。
- 不做当天多 slides 拼接长图。

## 5. Gemma 接入位置

后续大模型接入时，只替换“mock 生成 slide 内容”这一步：

```text
Record 提交图片 / 文字
  -> App 保存图片、提取主色
  -> 后端接收压缩图片 + 文本
  -> Gemma 返回结构化 JSON
  -> App 写入当天 entry 的新 slide
  -> Result 用本地图卡 renderer 展示
```

Gemma 负责：

- 理解图片和文字。
- 生成 `title`。
- 生成 `quote`。
- 生成 `caption / diary text`。
- 生成 `comfortText / 宝宝说`。
- 更新当天 `timelineSummary`。
- 返回必要的安全提醒。

Gemma 不负责：

- 最终图卡排版。
- 长图导出。
- 收藏、时间线、纪念册。
- 本地图片保存。
- AI 生图。

## 6. 验收与测试

单测 / DAO：

- 第一次提交创建当天 entry。
- 第二次提交追加 slide，不创建第二个同日期 entry。
- media 能从 Room 读回并关联到 slide。
- entry 级收藏会让当天进入 Memory Book。

UI：

- 文字-only 可提交。
- 图片-only 可提交。
- 两次提交后 Result 显示 `1/2`、`2/2`。
- Timeline 同一天仍只有一个入口。
- 分享预览只展示当前 slide。

验证命令：

```powershell
$env:JAVA_HOME='D:\software\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

真机连接并授权后再运行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 7. 2026-05-17 实施记录

这一节记录 Day 3 当天真实落地的关键改动，便于后续回溯。细小 UI 文案调整不单独列出。

### Record 输入链路

- 重写 Record 页，使第一屏更接近图片/文本输入工具，而不是说明页。
- 接入单图选择，将图片复制到 app 私有目录，并在提交时写入 `entry_media`。
- 每次进入 Record 页时，清空上一次导入但未生成 result 的图片、ROI 和临时主色状态。
- 支持图片 ROI 选择：以近似 1:1 的显示框为目标，允许用户移动长图中的裁切区域。
- 主色提取基于用户选定 ROI，而不是整张图片或错误的显示裁切区域。

### Result 渲染与编辑

- Result 页从单张 mock 图卡推进为当天多 slide 图册，支持横滑、页码和 dots。
- Result 图卡使用 `EntryMedia` 渲染用户图片；图片区域和上方主色区域保持稳定比例。
- 图卡上半部分使用图片 ROI 对应主色和 slide 渐变色。
- 地点和时间展示调整为同一区域：无定位权限时显示 `My City`，时间使用 `08:59 pm` 这类格式。
- 去掉长图预览单页场景中的 `1/1` 页码提示。
- 编辑能力扩展为 entry 级 `diaryTitle` + slide/note 级 `diaryText` 的一体编辑器。
- 标题编辑中允许临时为空，退出编辑时再补默认标题 `今天也留一页`。
- 修复 Result 编辑器输入后光标回到开头导致 `123` 变 `321` 的问题，编辑器改用 `TextFieldValue` 保存 selection。
- Result 编辑态下点击图卡、顶部操作、宝宝说卡片等热区会自动完成编辑。

### 宝宝说规则

- `宝宝说` 不再每条 record 都显示。
- 当前规则为关键词触发，或按约 0.2 概率触发。
- 关键词包括“累”“疲惫”“困”“胎动”“动了一下”“踢”等。

### Timeline / Memory 联动

- `diaryTitle` 明确为当天 `DiaryEntry` 级标题，Memory 和 Timeline 都读取 entry title。
- Memory 每条 entry 的解释文本读取第一张 slide 的第一条 note 文本，超出高度时省略。
- Timeline 解释文本改为从当天所有 slides/notes 中取第一条非空 `selfText`，不再直接展示旧的 `timelineSummary` 静态摘要。

### 删除单帧能力

- Result 页为用户生成的 slide 增加删除按钮。
- 删除前增加二次确认弹窗。
- 删除 slide 时，同步删除对应 note 文本、media 引用和本地图片文件。
- 如果当天最后一张 slide 被删除，则删除整个 entry 并回到首页。

### 分享长图预览与导出

- 分享入口改为 Result 页浮层预览，不再追加到页面下方。
- 长图预览支持滚动，避免内容超出后看不完整。
- 预览标题和最终导出标题统一使用 entry 级 `diaryTitle`。
- 导出图卡使用与 Result 图卡相同的 ROI 源区域、主色渐变和基础比例常量。
- 调整最终导出图中的宝宝说卡片和 `You & Me Diary` 署名位置，避免署名压到宝宝说框底部。

### 本地数据与状态

- 扩展 `EntryMedia`、Room entity、relation、mapper 和 DAO，使 media、ROI、edited self/baby text 能本地持久化。
- Repository 增加当天 append slide、删除 slide、更新 entry title + note text 的能力。
- ViewModel 负责 Record 临时图片状态、ROI 状态、提交后导航到 Result，以及删除当前 slide 后的导航修正。
- 补充或更新 mapper、DAO 和 UI 相关测试。

### 验证结果

本轮关键修改后已运行：

```powershell
$env:JAVA_HOME='D:\software\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

两项均通过。

## 8. 遗留风险 / 明日优先事项

- 分享长图仍然是两套渲染路径：预览使用 Compose，最终导出使用 Android Canvas 手写重画。两边字段、间距、行数和条件显示需要人工同步，容易再次出现预览和导出不一致。
- 当前已知问题：当 `宝宝说` 不显示时，最终导出图仍可能保留宝宝说区域对应的大块空白，不符合预期。下一步应修正导出布局，让无宝宝说时正文和署名自然上收。
- 更理想的后续方向是收敛分享预览和最终导出为同一份渲染描述，或抽出共享布局参数与显示条件，减少重复维护。
- Room schema 当前仍处于 MVP 快速迭代状态，复杂 migration 和向后兼容测试尚未补齐。
