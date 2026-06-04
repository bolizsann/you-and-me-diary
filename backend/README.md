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

当前测试服务部署在 Cloud Run：

```text
https://you-and-me-diary-api-7ofcf3aymq-de.a.run.app
```

`/health` 和 `/version` 公开；`/generate-diary` 与 `/transcribe-voice` 需要请求头 `X-App-Token`。`GEMINI_API_KEY` 和
`APP_API_TOKEN` 存在 Google Secret Manager，不要写进仓库。

部署后用 `/version` 确认线上代码版本：

```powershell
Invoke-RestMethod -Uri 'https://you-and-me-diary-api-7ofcf3aymq-de.a.run.app/version'
```

Windows 本机 gcloud 安装在 `D:\software\google-cloud-sdk`，配置目录在
`D:\software\gcloud-config`。如果访问 Google API 失败，先让 PowerShell 使用本机代理：

```powershell
$env:CLOUDSDK_CONFIG='D:\software\gcloud-config'
$env:HTTPS_PROXY='http://127.0.0.1:7890'
$env:HTTP_PROXY='http://127.0.0.1:7890'
```

使用云端地址打 debug 包时，从 Secret Manager 读取 token，避免明文落盘：

```powershell
$env:APP_API_TOKEN = (D:\software\google-cloud-sdk\bin\gcloud.cmd secrets versions access latest --secret=APP_API_TOKEN).Trim()
.\gradlew.bat :app:assembleDebug -PbackendBaseUrl=https://you-and-me-diary-api-7ofcf3aymq-de.a.run.app -PbackendAppToken="$env:APP_API_TOKEN"
```
