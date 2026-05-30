# You & Me Diary 后端

第 1 天后端保持非常轻量。Android App 当前使用本地 mock 数据；这个服务只用于验证后续 API 边界可以被启动和访问。

在整体架构中，FastAPI 位于轻后端/API 层。它不负责 Android 界面、本地存储或图卡渲染；后续主要负责接收 App 发来的文本/图片、调用 Gemma、校验模型输出 JSON，并把结构化日记结果返回给 App。

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

`/health` 公开；`/generate-diary` 需要请求头 `X-App-Token`。`GEMINI_API_KEY` 和
`APP_API_TOKEN` 存在 Google Secret Manager，不要写进仓库。

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
