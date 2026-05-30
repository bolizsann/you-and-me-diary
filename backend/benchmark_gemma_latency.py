from __future__ import annotations

import argparse
import base64
import json
import os
import statistics
import time
from dataclasses import dataclass
from datetime import datetime
from io import BytesIO
from pathlib import Path
from typing import Any

from google import genai
from google.genai import types
from PIL import Image

from generation_settings import DEFAULT_MODEL, max_output_tokens_for
from online_gemma_client import classify_gemma_error
from prompt import PROMPT_VERSION, build_generate_diary_prompt
from schemas import DiaryImageInput, GemmaDiaryPayload, GenerateDiaryRequest


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_IMAGES = [ROOT / "274270250.jpg", ROOT / "1784538846.jpg"]
DEFAULT_TEXT = "有点沉有点累，我会不会有点丑......"
FAST_MODELS = ["gemini-2.5-flash-lite", "gemini-2.5-flash"]


@dataclass(frozen=True)
class BenchCase:
    name: str
    model: str
    send_image: bool
    image_size: int
    jpeg_quality: int
    media_resolution: str | None = None


def main() -> None:
    args = parse_args()
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise SystemExit("GEMINI_API_KEY is not set.")

    images = [Path(path).resolve() for path in args.images]
    cases = build_cases(args)
    output_dir = ROOT / "backend" / "bench_results"
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"latency_{datetime.now().strftime('%Y%m%d_%H%M%S')}.jsonl"
    client = genai.Client(api_key=api_key)

    print(f"Running {len(cases)} cases x {len(images)} images x {args.repeats} repeats")
    print(f"Writing JSONL to {output_path}")

    records: list[dict[str, Any]] = []
    with output_path.open("w", encoding="utf-8") as output:
        for repeat in range(1, args.repeats + 1):
            for image_path in images:
                for case in cases:
                    record = run_case(
                        client=client,
                        case=case,
                        image_path=image_path,
                        text=args.text,
                        repeat=repeat,
                        stream=args.stream,
                    )
                    records.append(record)
                    output.write(json.dumps(record, ensure_ascii=False) + "\n")
                    output.flush()
                    print(format_record(record))

    print_summary(records)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Benchmark Gemma/Gemini diary generation latency.")
    parser.add_argument("--images", nargs="+", default=[str(path) for path in DEFAULT_IMAGES])
    parser.add_argument("--text", default=DEFAULT_TEXT)
    parser.add_argument("--repeats", type=int, default=2)
    parser.add_argument("--quick", action="store_true", help="Run one repeat and fewer cases.")
    parser.add_argument("--stream", action="store_true", help="Use streaming to measure first chunk latency.")
    parser.add_argument("--case", choices=["all", "baseline", "sizes", "no-image", "models", "media-low"], default="all")
    return parser.parse_args()


def build_cases(args: argparse.Namespace) -> list[BenchCase]:
    if args.quick:
        args.repeats = 1
        return [
            BenchCase("baseline_512_q78_image", DEFAULT_MODEL, True, 512, 78),
            BenchCase("fast_flash_lite_384_q75_image", FAST_MODELS[0], True, 384, 75),
            BenchCase("fast_flash_lite_no_image", FAST_MODELS[0], False, 0, 0),
            BenchCase("gemma_no_image", DEFAULT_MODEL, False, 0, 0),
        ]

    cases = [
        BenchCase("baseline_512_q78_image", DEFAULT_MODEL, True, 512, 78),
        BenchCase("gemma_384_q75_image", DEFAULT_MODEL, True, 384, 75),
        BenchCase("gemma_256_q72_image", DEFAULT_MODEL, True, 256, 72),
        BenchCase("gemma_no_image", DEFAULT_MODEL, False, 0, 0),
        BenchCase("model_gemma_384_q75_image", DEFAULT_MODEL, True, 384, 75),
        BenchCase("model_flash_lite_384_q75_image", FAST_MODELS[0], True, 384, 75),
        BenchCase("model_flash_lite_no_image", FAST_MODELS[0], False, 0, 0),
        BenchCase("model_flash_384_q75_image", FAST_MODELS[1], True, 384, 75),
        BenchCase("model_flash_no_image", FAST_MODELS[1], False, 0, 0),
        BenchCase("gemma_384_q75_media_low", DEFAULT_MODEL, True, 384, 75, "LOW"),
    ]
    if args.case == "all":
        return cases
    return [case for case in cases if case_matches(case, args.case)]


def case_matches(case: BenchCase, selected: str) -> bool:
    return {
        "baseline": case.name.startswith("baseline"),
        "sizes": case.name in {"baseline_512_q78_image", "gemma_384_q75_image", "gemma_256_q72_image"},
        "no-image": "no_image" in case.name,
        "models": case.name.startswith("model_"),
        "media-low": "media_low" in case.name,
    }[selected]


def run_case(
    client: genai.Client,
    case: BenchCase,
    image_path: Path,
    text: str,
    repeat: int,
    stream: bool,
) -> dict[str, Any]:
    image_info = prepare_image(image_path, case.image_size, case.jpeg_quality)
    request = build_request(case, image_info, text)
    prompt = build_generate_diary_prompt(request)
    contents: list[Any] = [prompt]
    if request.image:
        contents.append(
            types.Part.from_bytes(
                data=image_info["jpeg_bytes"],
                mime_type=request.image.mimeType,
            ),
        )

    record = {
        "repeat": repeat,
        "case": case.name,
        "model": case.model,
        "image": image_path.name,
        "originalWidth": image_info["original_width"],
        "originalHeight": image_info["original_height"],
        "compressedSize": case.image_size if case.send_image else 0,
        "jpegQuality": case.jpeg_quality if case.send_image else 0,
        "imagePayloadKb": round(len(image_info["jpeg_bytes"]) / 1024, 1) if case.send_image else 0,
        "requestPayloadKb": round(len(request.model_dump_json()) / 1024, 1),
        "dominantColor": image_info["dominant_color"],
        "promptVersion": PROMPT_VERSION,
        "maxOutputTokens": max_output_tokens_for(request.diaryTextMode),
        "mediaResolution": case.media_resolution or "default",
        "stream": stream,
    }

    started = time.perf_counter()
    first_chunk_seconds: float | None = None
    try:
        config = build_config(case, request)
        text_response = ""
        if stream:
            for chunk in client.models.generate_content_stream(
                model=case.model,
                contents=contents,
                config=config,
            ):
                if first_chunk_seconds is None:
                    first_chunk_seconds = time.perf_counter() - started
                text_response += chunk.text or ""
        else:
            response = client.models.generate_content(
                model=case.model,
                contents=contents,
                config=config,
            )
            text_response = response.text or ""

        elapsed = time.perf_counter() - started
        payload = GemmaDiaryPayload.model_validate_json(text_response)
        record.update(
            {
                "status": "ok",
                "elapsedSeconds": round(elapsed, 2),
                "firstChunkSeconds": round(first_chunk_seconds, 2) if first_chunk_seconds is not None else None,
                "titleSuggestion": payload.titleSuggestion,
                "diaryText": payload.diaryText,
                "cardSummary": payload.cardSummary,
                "cardEmoji": payload.cardEmoji,
                "babyText": payload.babyText,
                "babyTextPresent": bool(payload.babyText),
                "safetyNote": payload.safetyNote,
                "responseJson": payload.model_dump(),
                "errorType": "",
                "errorMessage": "",
            },
        )
    except Exception as exc:
        elapsed = time.perf_counter() - started
        error_type, error_message = classify_gemma_error(exc)
        record.update(
            {
                "status": "error",
                "elapsedSeconds": round(elapsed, 2),
                "firstChunkSeconds": round(first_chunk_seconds, 2) if first_chunk_seconds is not None else None,
                "titleSuggestion": "",
                "diaryText": "",
                "cardSummary": "",
                "cardEmoji": "",
                "babyText": "",
                "babyTextPresent": False,
                "safetyNote": "",
                "responseJson": {},
                "errorType": error_type,
                "errorMessage": error_message,
            },
        )
    return record


def prepare_image(image_path: Path, size: int, quality: int) -> dict[str, Any]:
    image = Image.open(image_path).convert("RGB")
    width, height = image.size
    crop_size = min(width, height)
    left = (width - crop_size) // 2
    top = (height - crop_size) // 2
    cropped = image.crop((left, top, left + crop_size, top + crop_size))
    target_size = max(size, 1)
    resized = cropped.resize((target_size, target_size), Image.Resampling.LANCZOS)
    output = BytesIO()
    resized.save(output, format="JPEG", quality=max(quality, 1), optimize=True)
    jpeg_bytes = output.getvalue()
    dominant_color = average_color(resized)
    image.close()
    cropped.close()
    resized.close()
    return {
        "original_width": width,
        "original_height": height,
        "jpeg_bytes": jpeg_bytes,
        "dominant_color": dominant_color,
    }


def average_color(image: Image.Image) -> str:
    sample = image.resize((24, 24), Image.Resampling.BILINEAR)
    pixels = list(sample.getdata())
    sample.close()
    red = sum(pixel[0] for pixel in pixels) // len(pixels)
    green = sum(pixel[1] for pixel in pixels) // len(pixels)
    blue = sum(pixel[2] for pixel in pixels) // len(pixels)
    return f"#{red:02X}{green:02X}{blue:02X}"


def build_request(case: BenchCase, image_info: dict[str, Any], text: str) -> GenerateDiaryRequest:
    image = None
    if case.send_image:
        image = DiaryImageInput(
            mimeType="image/jpeg",
            dataBase64=base64.b64encode(image_info["jpeg_bytes"]).decode("ascii"),
            dominantColor=image_info["dominant_color"],
        )
    return GenerateDiaryRequest(
        text=text,
        voiceText="",
        inputSource="typed",
        diaryTextMode="preserve",
        dateId="2026-05-23",
        dateLabel="5 月 23 日",
        currentTitle="",
        isFirstRecordForDay=True,
        username="你",
        estimatedDueDate=None,
        image=image,
    )


def build_config(case: BenchCase, request: GenerateDiaryRequest) -> types.GenerateContentConfig:
    media_resolution = None
    if case.media_resolution == "LOW":
        media_resolution = types.MediaResolution.MEDIA_RESOLUTION_LOW
    return types.GenerateContentConfig(
        max_output_tokens=max_output_tokens_for(request.diaryTextMode),
        response_mime_type="application/json",
        temperature=0.65,
        media_resolution=media_resolution,
    )


def format_record(record: dict[str, Any]) -> str:
    status = record["status"]
    detail = record["errorType"] if status == "error" else f"{record['cardSummary']}{record['cardEmoji']}"
    return (
        f"{record['case']:<32} {record['image']:<14} "
        f"{record['model']:<26} {record['elapsedSeconds']:>6.2f}s "
        f"{status:<5} {detail}"
    )


def print_summary(records: list[dict[str, Any]]) -> None:
    print("\nSummary")
    grouped: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for record in records:
        grouped.setdefault((record["case"], record["model"]), []).append(record)
    for (case_name, model), items in grouped.items():
        ok_times = [item["elapsedSeconds"] for item in items if item["status"] == "ok"]
        errors = [item for item in items if item["status"] != "ok"]
        if ok_times:
            median = statistics.median(ok_times)
            best = min(ok_times)
            worst = max(ok_times)
            print(f"- {case_name:<32} {model:<26} median={median:.2f}s best={best:.2f}s worst={worst:.2f}s errors={len(errors)}")
        else:
            error_type = errors[0]["errorType"] if errors else "unknown"
            print(f"- {case_name:<32} {model:<26} no success errors={len(errors)} firstError={error_type}")


if __name__ == "__main__":
    main()
