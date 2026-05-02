from fastapi import FastAPI

app = FastAPI(title="You & Me Diary API", version="0.1.0-day1")


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}
