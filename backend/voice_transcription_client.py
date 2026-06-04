import asyncio
import base64
import logging
import os
import time

from generation_settings import DEFAULT_MODEL, GEMMA_TIMEOUT_SECONDS
from online_gemma_client import classify_gemma_error
from schemas import TranscribeVoiceRequest, TranscribeVoiceResponse


logger = logging.getLogger("you_and_me_diary.voice")

MAX_AUDIO_BYTES = 12 * 1024 * 1024
VOICE_TRANSCRIPTION_MAX_ATTEMPTS = 2
VOICE_TRANSCRIPTION_RETRY_DELAY_SECONDS = 1.0


async def transcribe_voice(request: TranscribeVoiceRequest) -> TranscribeVoiceResponse:
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return build_fallback_response("missing_api_key", "GEMINI_API_KEY is not set.")

    model = os.getenv("GEMINI_TRANSCRIBE_MODEL", os.getenv("GEMINI_MODEL", DEFAULT_MODEL))
    try:
        started_at = time.perf_counter()
        result = await _transcribe_voice_with_retries(request, api_key, model)
        logger.info(
            "Voice transcription completed source=%s transcriptChars=%s elapsedSeconds=%.2f",
            result.source,
            len(result.transcript),
            time.perf_counter() - started_at,
        )
        return result
    except TimeoutError:
        logger.warning("Voice transcription timed out timeoutSeconds=%s", GEMMA_TIMEOUT_SECONDS)
        return build_fallback_response(
            "timeout",
            f"Voice transcription timed out after {GEMMA_TIMEOUT_SECONDS} seconds.",
        )
    except Exception as exc:
        error_type, error_message = classify_gemma_error(exc)
        logger.warning("Voice transcription failed errorType=%s error=%s", error_type, error_message)
        return build_fallback_response(error_type, error_message)


async def _transcribe_voice_with_retries(
    request: TranscribeVoiceRequest,
    api_key: str,
    model: str,
) -> TranscribeVoiceResponse:
    last_error: Exception | None = None
    for attempt in range(1, VOICE_TRANSCRIPTION_MAX_ATTEMPTS + 1):
        try:
            logger.info(
                "Calling voice transcription model=%s mimeType=%s locale=%s attempt=%s",
                model,
                request.mimeType,
                request.locale,
                attempt,
            )
            return await asyncio.wait_for(
                asyncio.to_thread(_transcribe_voice_sync, request, api_key),
                timeout=GEMMA_TIMEOUT_SECONDS,
            )
        except Exception as exc:
            last_error = exc
            error_type, error_message = classify_gemma_error(exc)
            if error_type != "model_temporarily_unavailable" or attempt >= VOICE_TRANSCRIPTION_MAX_ATTEMPTS:
                raise
            logger.warning(
                "Voice transcription transient failure attempt=%s errorType=%s error=%s",
                attempt,
                error_type,
                error_message,
            )
            await asyncio.sleep(VOICE_TRANSCRIPTION_RETRY_DELAY_SECONDS)
    if last_error:
        raise last_error
    raise RuntimeError("Voice transcription retry loop finished without a result.")


def _transcribe_voice_sync(request: TranscribeVoiceRequest, api_key: str) -> TranscribeVoiceResponse:
    from google import genai
    from google.genai import types

    audio_bytes = base64.b64decode(request.dataBase64, validate=True)
    if not audio_bytes:
        return build_fallback_response("empty_audio", "Audio payload is empty.")
    if len(audio_bytes) > MAX_AUDIO_BYTES:
        return build_fallback_response("audio_too_large", "Audio payload is too large.")

    client = genai.Client(api_key=api_key)
    model = os.getenv("GEMINI_TRANSCRIBE_MODEL", os.getenv("GEMINI_MODEL", DEFAULT_MODEL))
    prompt = (
        "请把这段音频转写成简体中文日记输入文本。"
        "只输出用户说出的内容，不要总结、润色、解释，也不要添加标点以外的额外文字。"
        f"如果音频不是中文，也按原语言尽量转写。locale={request.locale}"
    )
    response = client.models.generate_content(
        model=model,
        contents=[
            types.Part.from_bytes(data=audio_bytes, mime_type=request.mimeType),
            prompt,
        ],
        config=types.GenerateContentConfig(
            max_output_tokens=512,
            temperature=0.1,
        ),
    )
    transcript = clean_transcript(response.text or "")
    if not transcript:
        return build_fallback_response("empty_transcript", "Model returned an empty transcript.")
    return TranscribeVoiceResponse(transcript=transcript, source="gemini")


def build_fallback_response(error_type: str, error_message: str) -> TranscribeVoiceResponse:
    return TranscribeVoiceResponse(
        transcript="",
        source="fallback",
        debugErrorType=error_type,
        debugErrorMessage=error_message,
    )


def clean_transcript(text: str) -> str:
    clean = " ".join(text.strip().split())
    if len(clean) >= 2 and clean[0] in {"'", '"'} and clean[-1] == clean[0]:
        clean = clean[1:-1].strip()
    return clean
