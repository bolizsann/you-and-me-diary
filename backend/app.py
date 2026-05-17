from fastapi import FastAPI

from gemma_client import generate_diary
from schemas import GenerateDiaryRequest, GenerateDiaryResponse

app = FastAPI(title="You & Me Diary API", version="0.5")


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/generate-diary", response_model=GenerateDiaryResponse)
async def generate_diary_endpoint(request: GenerateDiaryRequest) -> GenerateDiaryResponse:
    return await generate_diary(request)
