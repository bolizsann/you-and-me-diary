import os

from fastapi import Depends, FastAPI, Header, HTTPException, status

from gemma_client import generate_diary
from schemas import GenerateDiaryRequest, GenerateDiaryResponse

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


@app.post(
    "/generate-diary",
    response_model=GenerateDiaryResponse,
    dependencies=[Depends(require_app_token)],
)
async def generate_diary_endpoint(request: GenerateDiaryRequest) -> GenerateDiaryResponse:
    return await generate_diary(request)
