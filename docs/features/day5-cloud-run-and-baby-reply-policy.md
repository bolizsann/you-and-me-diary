# Day 5：Cloud Run 部署、版本确认与宝宝回复策略

更新时间：2026-05-30

Day 5 是 Day 4 后端生成链路之后的一轮线上化和体验调优：把 FastAPI 部署到 Cloud Run，让手机不再依赖笔记本局域网后端；同时调整 `babyText` 的出现频率和内容池，避免每条记录都像客服式文本回复。

## 1. Cloud Run 部署

当前 Cloud Run 服务：

```text
project: gen-lang-client-0823926280
region: asia-east1
service: you-and-me-diary-api
```

后端部署文件：

```text
backend/Dockerfile
backend/.dockerignore
```

线上接口：

- `GET /health`：公开健康检查。
- `GET /version`：公开版本确认，返回 `apiVersion`、模型、`gitSha`、`sourceBuildStamp`。
- `POST /generate-diary`：需要 `X-App-Token`。

Secret Manager：

- `GEMINI_API_KEY`
- `APP_API_TOKEN`

Android 默认 `backendBaseUrl` 指向 Cloud Run；`backendAppToken` 仍通过 Gradle property 注入，不提交到仓库。

## 2. 版本确认策略

每次部署前先 commit，再把当前 commit hash 注入 Cloud Run：

```powershell
--set-env-vars GEMINI_MODEL=gemini-2.5-flash-lite,GIT_SHA=<git-sha>,SOURCE_BUILD_STAMP=<git-sha>
```

部署后检查：

```text
GET /version
```

期望返回的 `gitSha` 与本地 `git rev-parse --short HEAD` 一致。Cloud Run revision 名例如 `you-and-me-diary-api-00009-g6t` 只表示云端 revision，不足以直接证明对应哪次代码提交；`/version` 才是面向排查的代码版本确认入口。

## 3. 宝宝回复策略

目标：

- 快乐或中性记录里，宝宝不需要每条都输出完整文本，避免显得假。
- 悲伤、疲惫、胎动等更需要陪伴的记录里，宝宝更常输出文本。
- 轻回复可以是 emoji，也可以是短状态，例如“（蹬蹬腿）”“（呼噜呼噜）”“（翻身贴贴）”。
- 有安全提醒时清空 `babyText`，避免高风险孕期症状旁出现过度拟人化回应。

当前策略位于：

```text
backend/baby_reply_policy.py
```

情绪 mode：

- `happy`
- `neutral`
- `sad`
- `tired`
- `movement`

概率策略：

- `happy` / `neutral`：30% 文本，35% 轻回复，35% 不回复。
- `sad`：70% 文本，20% 轻回复，10% 不回复。
- `tired` / `movement`：60% 文本，25% 轻回复，15% 不回复。

稳定性：

- 使用 `dateId | inputSource | text | voiceText` 计算稳定 bucket。
- 同一条记录不会因为刷新或重新进入页面而随机改变宝宝回复。

内容池：

- 每个 mode 至少 10 个 emoji。
- 每个 mode 至少 30 个轻状态。
- 轻状态文字控制在 2-6 个中文字符，并用全角括号展示。

兜底与清洗：

- 如果模型在悲伤文本槽里返回“（小手挥挥）”这类轻状态，会替换为更合适的安慰文本。
- 如果非中性场景里模型返回“我在这里听着呢。”这类泛化文本，会替换为对应情绪的默认文本。
- 如果存在 `safetyNote`，最终 `babyText` 为空。

## 4. 后端文件边界

为后续端侧 local Gemma client 做准备，后端已拆分为：

```text
backend/online_gemma_client.py     云端 Gemma 调用、JSON 解析、错误分类
backend/generation_settings.py     默认模型、timeout、max output tokens
backend/diary_fallbacks.py         标题、正文、卡片和安全提醒 fallback
backend/baby_reply_policy.py       宝宝回复情绪与频率策略
backend/benchmark_gemma_latency.py 手动延迟 benchmark，不随 pytest 自动运行
```

未来如果实现端侧模型，可以新增 `local_gemma_client.py`，并复用 `diary_fallbacks.py` 与 `baby_reply_policy.py` 的后处理逻辑。

## 5. 测试覆盖

后端测试重点：

- `/health` 和 `/version` 公开可用。
- `/generate-diary` 必须带 `X-App-Token`。
- API key 缺失时走 fallback。
- 高风险文本返回安全提醒，并清空宝宝回复。
- 快乐/中性降低文本回复频率。
- 悲伤/疲惫/胎动提高文本回复频率。
- 轻状态和泛化文本会按情绪策略替换。
- 每个 mode 的 emoji 和轻状态数量满足下限。

验证命令：

```powershell
python -m pytest backend
python -m py_compile backend/benchmark_gemma_latency.py backend/online_gemma_client.py backend/baby_reply_policy.py backend/diary_fallbacks.py backend/generation_settings.py
```
