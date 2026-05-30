from fastapi.testclient import TestClient

from app import app
from prompt import build_generate_diary_prompt
from schemas import GenerateDiaryRequest


AUTH_HEADERS = {"X-App-Token": "test-token"}


def test_health() -> None:
    client = TestClient(app)

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


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
