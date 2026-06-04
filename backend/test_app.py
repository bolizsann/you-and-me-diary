from fastapi.testclient import TestClient

from app import app
from baby_reply_policy import (
    ALL_BABY_REACTIONS,
    HAPPY_BABY_REACTIONS,
    MOVEMENT_BABY_REACTIONS,
    NEUTRAL_BABY_REACTIONS,
    SAD_BABY_REACTIONS,
    TIRED_BABY_REACTIONS,
    apply_baby_reply_policy,
    baby_reply_bucket,
    choose_baby_reply,
    classify_baby_reply_mood,
)
from generation_settings import DEFAULT_MODEL
from diary_fallbacks import select_card_emoji_from_fields, select_card_summary
from online_gemma_client import classify_gemma_error
from prompt import build_generate_diary_prompt
from schemas import GenerateDiaryRequest, GenerateDiaryResponse


AUTH_HEADERS = {"X-App-Token": "test-token"}


def test_health() -> None:
    client = TestClient(app)

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_version_reports_runtime_version(monkeypatch) -> None:
    monkeypatch.setenv("GEMINI_MODEL", "test-model")
    monkeypatch.setenv("GIT_SHA", "abc1234")
    monkeypatch.setenv("SOURCE_BUILD_STAMP", "abc1234-dirty-test")
    client = TestClient(app)

    response = client.get("/version")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "apiVersion": app.version,
        "model": "test-model",
        "gitSha": "abc1234",
        "sourceBuildStamp": "abc1234-dirty-test",
    }


def test_version_uses_default_model_when_unset(monkeypatch) -> None:
    monkeypatch.delenv("GEMINI_MODEL", raising=False)
    client = TestClient(app)

    response = client.get("/version")

    assert response.status_code == 200
    assert response.json()["model"] == DEFAULT_MODEL


def test_generate_diary_rejects_missing_app_token(monkeypatch) -> None:
    monkeypatch.setenv("APP_API_TOKEN", "test-token")
    client = TestClient(app)

    response = client.post(
        "/generate-diary",
        json={
            "text": "今天真的有点累。",
            "dateId": "2026-05-18",
            "dateLabel": "5 月 18 日",
        },
    )

    assert response.status_code == 401


def test_generate_diary_requires_configured_app_token(monkeypatch) -> None:
    monkeypatch.delenv("APP_API_TOKEN", raising=False)
    client = TestClient(app)

    response = client.post(
        "/generate-diary",
        headers=AUTH_HEADERS,
        json={
            "text": "今天真的有点累。",
            "dateId": "2026-05-18",
            "dateLabel": "5 月 18 日",
        },
    )

    assert response.status_code == 503


def test_classifies_model_high_demand_as_temporary_unavailable() -> None:
    class FakeServerError(Exception):
        status_code = 503

    error_type, error_message = classify_gemma_error(
        FakeServerError("This model is currently experiencing high demand. Please try again later."),
    )

    assert error_type == "model_temporarily_unavailable"
    assert "high demand" in error_message


def test_transcribe_voice_rejects_missing_app_token(monkeypatch) -> None:
    monkeypatch.setenv("APP_API_TOKEN", "test-token")
    client = TestClient(app)

    response = client.post(
        "/transcribe-voice",
        json={
            "mimeType": "audio/mp4",
            "dataBase64": "",
            "locale": "zh-CN",
        },
    )

    assert response.status_code == 401


def test_transcribe_voice_requires_configured_app_token(monkeypatch) -> None:
    monkeypatch.delenv("APP_API_TOKEN", raising=False)
    client = TestClient(app)

    response = client.post(
        "/transcribe-voice",
        headers=AUTH_HEADERS,
        json={
            "mimeType": "audio/mp4",
            "dataBase64": "",
            "locale": "zh-CN",
        },
    )

    assert response.status_code == 503


def test_transcribe_voice_falls_back_without_api_key(monkeypatch) -> None:
    monkeypatch.setenv("APP_API_TOKEN", "test-token")
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    client = TestClient(app)

    response = client.post(
        "/transcribe-voice",
        headers=AUTH_HEADERS,
        json={
            "mimeType": "audio/mp4",
            "dataBase64": "",
            "locale": "zh-CN",
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["source"] == "fallback"
    assert data["transcript"] == ""
    assert data["debugErrorType"] == "missing_api_key"


def test_generate_diary_falls_back_without_api_key(monkeypatch) -> None:
    monkeypatch.setenv("APP_API_TOKEN", "test-token\n")
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    client = TestClient(app)

    response = client.post(
        "/generate-diary",
        headers=AUTH_HEADERS,
        json={
            "text": "今天真的有点累。",
            "voiceText": "",
            "inputSource": "typed",
            "diaryTextMode": "preserve",
            "dateId": "2026-05-18",
            "dateLabel": "5 月 18 日",
            "currentTitle": "",
            "isFirstRecordForDay": True,
            "username": "你",
            "estimatedDueDate": None,
            "image": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["source"] == "fallback"
    assert data["titleSuggestion"] == "慢一点也认真"
    assert data["diaryText"] == "今天真的有点累。"
    assert data["cardSummary"] == "有点累了"
    assert data["cardEmoji"] == "☁️"
    assert data["babyText"]


def test_generate_diary_returns_safety_note_for_high_risk_text(monkeypatch) -> None:
    monkeypatch.setenv("APP_API_TOKEN", "test-token")
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    client = TestClient(app)

    response = client.post(
        "/generate-diary",
        headers=AUTH_HEADERS,
        json={
            "text": "今天有出血，还有剧烈疼痛。",
            "voiceText": "",
            "inputSource": "typed",
            "diaryTextMode": "preserve",
            "dateId": "2026-05-18",
            "dateLabel": "5 月 18 日",
            "isFirstRecordForDay": True,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["safetyNote"]
    assert "医生" in data["safetyNote"]
    assert "药" not in data["safetyNote"]


def test_prompt_records_title_suggestion_policy_for_later_records() -> None:
    prompt = build_generate_diary_prompt(
        GenerateDiaryRequest(
            text="又补了一张下午的照片。",
            dateId="2026-05-18",
            dateLabel="5 月 18 日",
            currentTitle="慢一点也认真",
            isFirstRecordForDay=False,
        ),
    )

    assert "titleSuggestion 只是建议" in prompt
    assert "不会覆盖当前标题" in prompt
    assert "只输出 JSON" in prompt


def test_prompt_keeps_baby_text_short_and_conditional() -> None:
    prompt = build_generate_diary_prompt(
        GenerateDiaryRequest(
            text="今天很累，但是也感觉到一点胎动。",
            inputSource="typed",
            diaryTextMode="preserve",
            dateId="2026-05-18",
            dateLabel="5 月 18 日",
            isFirstRecordForDay=True,
        ),
    )

    assert "累" in prompt
    assert "胎动" in prompt
    assert "diaryTextMode = preserve" in prompt
    assert "不要润色、扩写" in prompt
    assert "cardSummary" in prompt
    assert "cardEmoji" in prompt
    assert "不是每条都必须出现" in prompt
    assert "快乐或语气很平稳" in prompt
    assert "（噗噜噗噜）" in prompt
    assert "12-36 个中文字符" in prompt
    assert "不要把医疗提醒写进 babyText" in prompt


def test_prompt_omits_weird_due_date_placeholder() -> None:
    prompt = build_generate_diary_prompt(
        GenerateDiaryRequest(
            text="今天只是想记录一下。",
            inputSource="typed",
            diaryTextMode="preserve",
            dateId="2026-05-18",
            dateLabel="5 月 18 日",
            estimatedDueDate=None,
        ),
    )

    assert "estimatedDueDate：未提供" in prompt
    assert "None" not in prompt
    assert "estimatedDueDate：null" not in prompt


def test_generate_diary_polishes_long_voice_text_in_fallback(monkeypatch) -> None:
    monkeypatch.setenv("APP_API_TOKEN", "test-token")
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    client = TestClient(app)

    response = client.post(
        "/generate-diary",
        headers=AUTH_HEADERS,
        json={
            "text": "",
            "voiceText": "今天说了好多好多话，感觉有点乱但是也想留下来。",
            "inputSource": "voice",
            "diaryTextMode": "polish",
            "dateId": "2026-05-18",
            "dateLabel": "5 月 18 日",
            "isFirstRecordForDay": True,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["source"] == "fallback"
    assert data["diaryText"].startswith("这一页先把今天的话轻轻收好")


def test_card_summary_rejects_full_input_and_manual_emoji() -> None:
    request = GenerateDiaryRequest(
        text="😊",
        voiceText="今天宝宝动了一下，我特别开心。",
        inputSource="mixed",
        diaryTextMode="polish",
        dateId="2026-06-04",
        dateLabel="6 月 4 日",
    )

    assert select_card_summary(request, "😊 今天宝宝动了一下，我特别开心。") == "好开心啊"
    assert select_card_emoji_from_fields(request, "😊 今天宝宝动了一下，我特别开心。", "") == "😊"
    assert select_card_emoji_from_fields(request, "今天宝宝动了一下，我特别开心。", "✨✨✨") == "✨"


def test_baby_reply_policy_is_stable_for_same_record() -> None:
    request = GenerateDiaryRequest(
        text="猫猫真可爱，我很开心有它陪在我身边",
        inputSource="typed",
        diaryTextMode="preserve",
        dateId="2026-05-25",
        dateLabel="5 月 25 日",
    )

    assert baby_reply_bucket(request) == baby_reply_bucket(request)


def test_baby_reply_policy_can_soften_happy_text_to_reaction() -> None:
    request = GenerateDiaryRequest(
        text="猫猫真可爱，我很开心有它陪在我身边",
        inputSource="typed",
        diaryTextMode="preserve",
        dateId="2026-05-25",
        dateLabel="5 月 25 日",
    )
    result = GenerateDiaryResponse(
        titleSuggestion="有猫咪真好",
        diaryText=request.text,
        cardSummary="有猫咪真好",
        cardEmoji="🐱",
        babyText="有它陪着你，真好呀。",
    )

    processed = apply_baby_reply_policy(request, result)

    assert classify_baby_reply_mood(request, result) == "happy"
    assert processed.babyText in {*HAPPY_BABY_REACTIONS, ""}


def test_baby_reply_policy_keeps_more_text_for_sad_records() -> None:
    request = GenerateDiaryRequest(
        text="今天是520，老公之前给我买过花，但是今天却没有",
        inputSource="typed",
        diaryTextMode="preserve",
        dateId="2026-05-25",
        dateLabel="5 月 25 日",
    )
    result = GenerateDiaryResponse(
        titleSuggestion="今天的花",
        diaryText=request.text,
        cardSummary="有点小失落",
        cardEmoji="😔",
        babyText="我在这里陪着你。",
    )

    processed = apply_baby_reply_policy(request, result)

    assert classify_baby_reply_mood(request, result) == "sad"
    assert processed.babyText in {"我在这里陪着你。", *SAD_BABY_REACTIONS, ""}


def test_baby_reply_policy_replaces_light_reaction_in_sad_text_slot() -> None:
    request = GenerateDiaryRequest(
        text="今天是520，老公之前给我买过花，但是今天却没有",
        inputSource="typed",
        diaryTextMode="preserve",
        dateId="2026-05-30",
        dateLabel="5 月 30 日",
    )
    result = GenerateDiaryResponse(
        titleSuggestion="今天的花",
        diaryText=request.text,
        cardSummary="520",
        cardEmoji="💖",
        babyText="（小手挥挥）",
    )

    processed = apply_baby_reply_policy(request, result)

    assert classify_baby_reply_mood(request, result) == "sad"
    assert baby_reply_bucket(request) < 70
    assert processed.babyText == "我在这里陪着你。"


def test_baby_reply_policy_replaces_generic_text_in_non_neutral_text_slot() -> None:
    request = GenerateDiaryRequest(
        text="猫猫真可爱，我很开心有它陪在我身边",
        inputSource="typed",
        diaryTextMode="preserve",
        dateId="2026-05-02",
        dateLabel="5 月 2 日",
    )
    result = GenerateDiaryResponse(
        titleSuggestion="有猫咪真好",
        diaryText=request.text,
        cardSummary="",
        cardEmoji="",
        babyText="我在这里听着呢。",
    )

    processed = apply_baby_reply_policy(request, result)

    assert classify_baby_reply_mood(request, result) == "happy"
    assert baby_reply_bucket(request) < 30
    assert processed.babyText == "我也跟着开心了一下。"


def test_choose_baby_reply_thresholds() -> None:
    assert choose_baby_reply(29, 30, 65, "我在这里听着呢。", ("✨",)) == "我在这里听着呢。"
    assert choose_baby_reply(30, 30, 65, "我在这里听着呢。", ("✨",)) == "✨"
    assert choose_baby_reply(65, 30, 65, "我在这里听着呢。", ("✨",)) == ""


def test_baby_reaction_pools_have_enough_variety() -> None:
    for reactions in [
        HAPPY_BABY_REACTIONS,
        NEUTRAL_BABY_REACTIONS,
        SAD_BABY_REACTIONS,
        TIRED_BABY_REACTIONS,
        MOVEMENT_BABY_REACTIONS,
    ]:
        emoji_count = sum(1 for item in reactions if not item.startswith("（"))
        state_count = sum(1 for item in reactions if item.startswith("（"))
        assert len(reactions) == len(set(reactions))
        assert emoji_count >= 10
        assert state_count >= 30
        assert all(2 <= len(item.removeprefix("（").removesuffix("）")) <= 6 for item in reactions if item.startswith("（"))
    assert set(ALL_BABY_REACTIONS).issuperset(HAPPY_BABY_REACTIONS)


def test_baby_reply_policy_clears_text_for_safety_note() -> None:
    request = GenerateDiaryRequest(
        text="今天有出血，还有剧烈疼痛。",
        inputSource="typed",
        diaryTextMode="preserve",
        dateId="2026-05-25",
        dateLabel="5 月 25 日",
    )
    result = GenerateDiaryResponse(
        titleSuggestion="慢一点也认真",
        diaryText=request.text,
        cardSummary="抱抱今天",
        cardEmoji="🤍",
        babyText="我陪着你。",
        safetyNote="如果这些感受持续或加重，建议及时联系医生或产检机构确认。",
    )

    processed = apply_baby_reply_policy(request, result)

    assert processed.safetyNote
    assert processed.babyText == ""
