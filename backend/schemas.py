from typing import Literal

from pydantic import BaseModel, Field


class DiaryImageInput(BaseModel):
    mimeType: str = Field(default="image/jpeg")
    dataBase64: str
    dominantColor: str | None = None


class GenerateDiaryRequest(BaseModel):
    text: str = ""
    voiceText: str = ""
    inputSource: Literal["typed", "voice", "mixed", "imageOnly"] = "typed"
    diaryTextMode: Literal["preserve", "polish", "generate"] = "preserve"
    dateId: str
    dateLabel: str
    currentTitle: str = ""
    isFirstRecordForDay: bool = True
    username: str = "你"
    estimatedDueDate: str | None = None
    image: DiaryImageInput | None = None


class TranscribeVoiceRequest(BaseModel):
    mimeType: str = Field(default="audio/mp4")
    dataBase64: str
    locale: str = "zh-CN"


class TranscribeVoiceResponse(BaseModel):
    transcript: str = ""
    source: Literal["gemini", "fallback"] = "fallback"
    debugErrorType: str = ""
    debugErrorMessage: str = ""


class GenerateDiaryResponse(BaseModel):
    titleSuggestion: str
    diaryText: str
    cardSummary: str = ""
    cardEmoji: str = ""
    babyText: str = ""
    safetyNote: str = ""
    source: Literal["gemma", "fallback", "mock"] = "fallback"
    debugErrorType: str = ""
    debugErrorMessage: str = ""


class GemmaDiaryPayload(BaseModel):
    titleSuggestion: str
    diaryText: str
    cardSummary: str = ""
    cardEmoji: str = ""
    babyText: str = ""
    safetyNote: str = ""
