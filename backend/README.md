# You & Me Diary 后端

后端保持轻量，主要承担 API 边界、prompt 构造、云端模型调用、结构化输出校验和 fallback。Android App 仍负责界面、本地存储和图卡渲染。

在整体架构中，FastAPI 位于轻后端/API 层。它接收 App 发来的文本/图片、调用模型、校验模型输出 JSON，并把结构化日记结果返回给 App。

## 运行

```powershell
cd backend
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app:app --reload --host 0.0.0.0 --port 8000
```

## 冒烟测试

```powershell
curl http://127.0.0.1:8000/health
```

预期响应：

```json
{"status":"ok"}
```

## Cloud Run

当前测试服务部署在 Cloud Run。具体服务地址和版本确认命令见 `docs/build.md`；需要确认当前线上 revision 时，用 `/version` 或 `gcloud run services describe`。

`/health` 和 `/version` 公开；`/generate-diary` 与 `/transcribe-voice` 需要请求头 `X-App-Token`。`GEMINI_API_KEY` 和
`APP_API_TOKEN` 存在 Google Secret Manager，不要写进仓库。

Windows 本机 gcloud 安装在 `D:\software\google-cloud-sdk`，配置目录在
`D:\software\gcloud-config`。如果访问 Google API 失败，先让 PowerShell 使用本机代理：

```powershell
$env:CLOUDSDK_CONFIG='D:\software\gcloud-config'
$env:HTTPS_PROXY='http://127.0.0.1:7890'
$env:HTTP_PROXY='http://127.0.0.1:7890'
```

使用云端地址打 debug 包时，后端 URL 和 app token 的注入方式见 `docs/build.md`，不要把 token 写进仓库。
