import os

from fastapi import Depends, FastAPI, Header, HTTPException, status

from generation_settings import DEFAULT_MODEL
from online_gemma_client import generate_diary
from schemas import (
    GenerateDiaryRequest,
    GenerateDiaryResponse,
    TranscribeVoiceRequest,
    TranscribeVoiceResponse,
)
from voice_transcription_client import transcribe_voice

app = FastAPI(title="You & Me Diary API", version="0.6")


def require_app_token(x_app_token: str | None = Header(default=None)) -> None:
    expected_token = (os.getenv("APP_API_TOKEN") or "").strip()
    if not expected_token:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="APP_API_TOKEN is not configured.",
        )
    if (x_app_token or "").strip() != expected_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid app token.",
        )


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/version")
async def version() -> dict[str, str]:
    return {
        "status": "ok",
        "apiVersion": app.version,
        "model": os.getenv("GEMINI_MODEL", DEFAULT_MODEL),
        "gitSha": os.getenv("GIT_SHA", "unknown"),
        "sourceBuildStamp": os.getenv("SOURCE_BUILD_STAMP", "unknown"),
    }


@app.post(
    "/generate-diary",
    response_model=GenerateDiaryResponse,
    dependencies=[Depends(require_app_token)],
)
async def generate_diary_endpoint(request: GenerateDiaryRequest) -> GenerateDiaryResponse:
    return await generate_diary(request)


@app.post(
    "/transcribe-voice",
    response_model=TranscribeVoiceResponse,
    dependencies=[Depends(require_app_token)],
)
async def transcribe_voice_endpoint(request: TranscribeVoiceRequest) -> TranscribeVoiceResponse:
    return await transcribe_voice(request)
