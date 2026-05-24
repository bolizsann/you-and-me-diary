import asyncio
import base64
import json
import logging
import os
import re
import time
from typing import Any

from prompt import build_generate_diary_prompt
from schemas import GenerateDiaryRequest, GenerateDiaryResponse, GemmaDiaryPayload


DEFAULT_MODEL = "gemini-2.5-flash-lite"
GEMMA_TIMEOUT_SECONDS = 45
GEMMA_DEFAULT_MAX_OUTPUT_TOKENS = 640
GEMMA_PRESERVE_MAX_OUTPUT_TOKENS = 384
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
    except TimeoutError as exc:
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
            max_output_tokens=max_output_tokens_for(request),
            response_mime_type="application/json",
            temperature=0.65,
        ),
    )
    payload = GemmaDiaryPayload.model_validate(_parse_json_object(response.text or ""))
    data = payload.model_dump()
    return GenerateDiaryResponse(
        titleSuggestion=str(data.get("titleSuggestion", "")).strip() or fallback_title(request),
        diaryText=select_diary_text(request, str(data.get("diaryText", "")).strip()),
        cardSummary=str(data.get("cardSummary", "")).strip() or fallback_card_summary(request),
        cardEmoji=str(data.get("cardEmoji", "")).strip() or fallback_card_emoji(request),
        babyText=str(data.get("babyText", "")).strip(),
        safetyNote=str(data.get("safetyNote", "")).strip() or safety_note_for(request),
        source="gemma",
    )


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


def max_output_tokens_for(request: GenerateDiaryRequest) -> int:
    if request.diaryTextMode == "preserve":
        return GEMMA_PRESERVE_MAX_OUTPUT_TOKENS
    return GEMMA_DEFAULT_MAX_OUTPUT_TOKENS


def build_fallback_response(
    request: GenerateDiaryRequest,
    error_type: str = "",
    error_message: str = "",
) -> GenerateDiaryResponse:
    return GenerateDiaryResponse(
        titleSuggestion=fallback_title(request),
        diaryText=fallback_diary_text(request),
        cardSummary=fallback_card_summary(request),
        cardEmoji=fallback_card_emoji(request),
        babyText=fallback_baby_text(request),
        safetyNote=safety_note_for(request),
        source="fallback",
        debugErrorType=error_type,
        debugErrorMessage=error_message,
    )


def classify_gemma_error(exc: Exception) -> tuple[str, str]:
    class_name = exc.__class__.__name__
    status_code = getattr(exc, "status_code", None) or getattr(exc, "code", None)
    raw_message = str(exc) or repr(exc)
    message = sanitize_error_message(raw_message)
    haystack = f"{class_name} {status_code or ''} {raw_message}".lower()

    if status_code == 429 or "quota" in haystack or "rate limit" in haystack or "resource_exhausted" in haystack:
        return "quota_or_rate_limit", f"{class_name}: {message}"
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


def fallback_title(request: GenerateDiaryRequest) -> str:
    combined = combined_text(request)
    if has_any(combined, "胎动", "动了一下", "踢"):
        return "一次小小胎动"
    if has_any(combined, "累", "疲惫", "困", "撑不住"):
        return "慢一点也认真"
    if request.image:
        return "今天看见的颜色"
    return "今天也留一页"


def fallback_diary_text(request: GenerateDiaryRequest) -> str:
    combined = combined_text(request).strip()
    if request.diaryTextMode == "preserve" and combined:
        return combined
    if combined:
        return f"这一页先把今天的话轻轻收好：{combined}"
    if request.image:
        return "这张图里有今天的光、颜色和一点点心情。先把它留下来，就已经很好。"
    return "今天也被认真地留在这里。"


def select_diary_text(request: GenerateDiaryRequest, generated_text: str) -> str:
    if request.diaryTextMode == "preserve":
        preserved = combined_text(request).strip()
        if preserved:
            return preserved
    return generated_text or fallback_diary_text(request)


def fallback_card_summary(request: GenerateDiaryRequest) -> str:
    combined = combined_text(request)
    if has_any(combined, "开心", "高兴", "快乐", "幸福"):
        return "好开心啊"
    if has_any(combined, "伤心", "难过", "委屈", "害怕"):
        return "抱抱今天"
    if has_any(combined, "胎动", "动了一下", "踢"):
        return "小小胎动"
    if has_any(combined, "累", "疲惫", "困", "撑不住"):
        return "有点累了"
    return ""


def fallback_card_emoji(request: GenerateDiaryRequest) -> str:
    combined = combined_text(request)
    if has_any(combined, "开心", "高兴", "快乐", "幸福"):
        return "😊"
    if has_any(combined, "伤心", "难过", "委屈", "害怕"):
        return "🤍"
    if has_any(combined, "胎动", "动了一下", "踢"):
        return "✨"
    if has_any(combined, "累", "疲惫", "困", "撑不住"):
        return "☁️"
    return ""


def fallback_baby_text(request: GenerateDiaryRequest) -> str:
    combined = combined_text(request)
    if has_any(combined, "胎动", "动了一下", "踢"):
        return "妈妈，我也在轻轻回应你。"
    if has_any(combined, "累", "疲惫", "困", "撑不住"):
        return "妈妈辛苦啦，我陪你慢慢来。"
    return ""


def safety_note_for(request: GenerateDiaryRequest) -> str:
    combined = combined_text(request)
    if has_any(combined, "出血", "流血", "剧烈疼痛", "胎动明显减少", "持续头晕", "明显加重"):
        return "如果这些感受持续或加重，建议及时联系医生或产检机构确认。"
    return ""


def combined_text(request: GenerateDiaryRequest) -> str:
    return " ".join(part for part in [request.text, request.voiceText] if part)


def has_any(text: str, *keywords: str) -> bool:
    return any(keyword in text for keyword in keywords)
