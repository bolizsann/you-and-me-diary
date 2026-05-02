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
