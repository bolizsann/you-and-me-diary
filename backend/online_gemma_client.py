import asyncio
import base64
import json
import logging
import os
import re
import time
from typing import Any

from baby_reply_policy import apply_baby_reply_policy
from diary_fallbacks import (
    fallback_card_emoji,
    fallback_card_summary,
    fallback_diary_text,
    fallback_title,
    safety_note_for,
    select_card_emoji_from_fields,
    select_card_summary,
    select_diary_text,
)
from generation_settings import (
    DEFAULT_MODEL,
    GEMMA_TIMEOUT_SECONDS,
    max_output_tokens_for,
)
from prompt import build_generate_diary_prompt
from schemas import GenerateDiaryRequest, GenerateDiaryResponse, GemmaDiaryPayload


logger = logging.getLogger("you_and_me_diary.gemma")


async def generate_diary(request: GenerateDiaryRequest) -> GenerateDiaryResponse:
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        result = build_fallback_response(
            request,
            error_type="missing_api_key",
            error_message="GEMINI_API_KEY is not set.",
        )
        log_generation_result(result)
        return result

    try:
        model = os.getenv("GEMINI_MODEL", DEFAULT_MODEL)
        logger.info("Calling Gemma model=%s", model)
        started_at = time.perf_counter()
        result = await asyncio.wait_for(
            asyncio.to_thread(_generate_diary_sync, request, api_key),
            timeout=GEMMA_TIMEOUT_SECONDS,
        )
        logger.info(
            "Gemma call completed model=%s elapsedSeconds=%.2f",
            model,
            time.perf_counter() - started_at,
        )
        log_generation_result(result)
        return result
    except TimeoutError:
        logger.warning(
            "Gemma generation timed out model=%s timeoutSeconds=%s",
            os.getenv("GEMINI_MODEL", DEFAULT_MODEL),
            GEMMA_TIMEOUT_SECONDS,
        )
        result = build_fallback_response(
            request,
            error_type="timeout",
            error_message=f"Gemma request timed out after {GEMMA_TIMEOUT_SECONDS} seconds.",
        )
        log_generation_result(result)
        return result
    except Exception as exc:
        error_type, error_message = classify_gemma_error(exc)
        logger.warning(
            "Gemma generation failed model=%s errorType=%s error=%s",
            os.getenv("GEMINI_MODEL", DEFAULT_MODEL),
            error_type,
            error_message,
        )
        result = build_fallback_response(request, error_type=error_type, error_message=error_message)
        log_generation_result(result)
        return result


def log_generation_result(result: GenerateDiaryResponse) -> None:
    logger.info(
        "Diary generation result source=%s cardSummary=%r cardEmoji=%r babyTextPresent=%s errorType=%s",
        result.source,
        result.cardSummary,
        result.cardEmoji,
        bool(result.babyText),
        result.debugErrorType,
    )


def _generate_diary_sync(request: GenerateDiaryRequest, api_key: str) -> GenerateDiaryResponse:
    from google import genai
    from google.genai import types

    client = genai.Client(api_key=api_key)
    model = os.getenv("GEMINI_MODEL", DEFAULT_MODEL)
    prompt = build_generate_diary_prompt(request)
    contents: list[Any] = [prompt]

    if request.image:
        image_bytes = base64.b64decode(request.image.dataBase64)
        contents.append(
            types.Part.from_bytes(
                data=image_bytes,
                mime_type=request.image.mimeType,
            ),
        )

    response = client.models.generate_content(
        model=model,
        contents=contents,
        config=types.GenerateContentConfig(
            max_output_tokens=max_output_tokens_for(request.diaryTextMode),
            response_mime_type="application/json",
            temperature=0.65,
        ),
    )
    payload = GemmaDiaryPayload.model_validate(_parse_json_object(response.text or ""))
    data = payload.model_dump()
    generated_card_summary = str(data.get("cardSummary", ""))
    generated_card_emoji = str(data.get("cardEmoji", ""))
    result = GenerateDiaryResponse(
        titleSuggestion=str(data.get("titleSuggestion", "")).strip() or fallback_title(request),
        diaryText=select_diary_text(request, str(data.get("diaryText", "")).strip()),
        cardSummary=select_card_summary(request, generated_card_summary),
        cardEmoji=select_card_emoji_from_fields(request, generated_card_summary, generated_card_emoji),
        babyText=str(data.get("babyText", "")).strip(),
        safetyNote=str(data.get("safetyNote", "")).strip() or safety_note_for(request),
        source="gemma",
    )
    return apply_baby_reply_policy(request, result)


def _parse_json_object(text: str) -> dict[str, Any]:
    clean = text.strip()
    fenced = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", clean, flags=re.DOTALL)
    if fenced:
        clean = fenced.group(1)
    elif "{" in clean and "}" in clean:
        clean = clean[clean.find("{") : clean.rfind("}") + 1]
    data = json.loads(clean)
    if not isinstance(data, dict):
        raise ValueError("Gemma response is not a JSON object")
    return data


def build_fallback_response(
    request: GenerateDiaryRequest,
    error_type: str = "",
    error_message: str = "",
) -> GenerateDiaryResponse:
    result = GenerateDiaryResponse(
        titleSuggestion=fallback_title(request),
        diaryText=fallback_diary_text(request),
        cardSummary=fallback_card_summary(request),
        cardEmoji=fallback_card_emoji(request),
        babyText="",
        safetyNote=safety_note_for(request),
        source="fallback",
        debugErrorType=error_type,
        debugErrorMessage=error_message,
    )
    return apply_baby_reply_policy(request, result)


def classify_gemma_error(exc: Exception) -> tuple[str, str]:
    class_name = exc.__class__.__name__
    status_code = getattr(exc, "status_code", None) or getattr(exc, "code", None)
    raw_message = str(exc) or repr(exc)
    message = sanitize_error_message(raw_message)
    haystack = f"{class_name} {status_code or ''} {raw_message}".lower()

    if status_code == 429 or "quota" in haystack or "rate limit" in haystack or "resource_exhausted" in haystack:
        return "quota_or_rate_limit", f"{class_name}: {message}"
    if status_code == 503 or "unavailable" in haystack or "high demand" in haystack or "overloaded" in haystack:
        return "model_temporarily_unavailable", f"{class_name}: {message}"
    if status_code in (401, 403) or "api_key_invalid" in haystack or "permission" in haystack or "unauthorized" in haystack:
        return "auth_or_permission", f"{class_name}: {message}"
    if status_code == 404 or "not found" in haystack or "not supported" in haystack or "model" in haystack and "not" in haystack:
        return "model_not_found_or_unsupported", f"{class_name}: {message}"
    if status_code == 400 or "invalid argument" in haystack or "bad request" in haystack:
        return "invalid_request", f"{class_name}: {message}"
    if "timeout" in haystack or "timed out" in haystack:
        return "timeout", f"{class_name}: {message}"
    if "connection" in haystack or "network" in haystack or "ssl" in haystack:
        return "network_error", f"{class_name}: {message}"
    return "unknown_gemma_error", f"{class_name}: {message}"


def sanitize_error_message(message: str) -> str:
    compact = " ".join(message.split())
    return compact[:500]
