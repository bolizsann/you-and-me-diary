import hashlib

from diary_fallbacks import combined_text, fallback_baby_text, has_any
from schemas import GenerateDiaryRequest, GenerateDiaryResponse


def apply_baby_reply_policy(
    request: GenerateDiaryRequest,
    result: GenerateDiaryResponse,
) -> GenerateDiaryResponse:
    if result.safetyNote:
        return result.model_copy(update={"babyText": ""})

    mood = classify_baby_reply_mood(request, result)
    bucket = baby_reply_bucket(request)
    candidate = result.babyText.strip() or fallback_baby_text(request)

    if mood == "sad":
        text_candidate = text_baby_candidate(candidate, "我在这里陪着你。", reject_generic=True)
        baby_text = choose_baby_reply(
            bucket=bucket,
            text_threshold=70,
            reaction_threshold=90,
            candidate=text_candidate,
            reactions=SAD_BABY_REACTIONS,
        )
    elif mood in {"tired", "movement"}:
        default_candidate = (
            "妈妈，我也在轻轻回应你。"
            if mood == "movement"
            else "妈妈辛苦啦，我陪你慢慢来。"
        )
        text_candidate = text_baby_candidate(candidate, default_candidate, reject_generic=True)
        baby_text = choose_baby_reply(
            bucket=bucket,
            text_threshold=60,
            reaction_threshold=85,
            candidate=text_candidate,
            reactions=MOVEMENT_BABY_REACTIONS if mood == "movement" else TIRED_BABY_REACTIONS,
        )
    elif mood == "happy":
        text_candidate = text_baby_candidate(candidate, "我也跟着开心了一下。", reject_generic=True)
        baby_text = choose_baby_reply(
            bucket=bucket,
            text_threshold=30,
            reaction_threshold=65,
            candidate=text_candidate,
            reactions=HAPPY_BABY_REACTIONS,
        )
    else:
        text_candidate = text_baby_candidate(candidate, "我在这里听着呢。")
        baby_text = choose_baby_reply(
            bucket=bucket,
            text_threshold=30,
            reaction_threshold=65,
            candidate=text_candidate,
            reactions=NEUTRAL_BABY_REACTIONS,
        )
    return result.model_copy(update={"babyText": baby_text})


def choose_baby_reply(
    bucket: int,
    text_threshold: int,
    reaction_threshold: int,
    candidate: str,
    reactions: tuple[str, ...],
) -> str:
    if bucket < text_threshold:
        return candidate
    if bucket < reaction_threshold:
        return reactions[bucket % len(reactions)]
    return ""


def text_baby_candidate(candidate: str, fallback: str, reject_generic: bool = False) -> str:
    clean = candidate.strip()
    if not clean or is_light_reaction(clean) or reject_generic and clean in GENERIC_BABY_TEXTS:
        return fallback
    return clean


def is_light_reaction(text: str) -> bool:
    clean = text.strip()
    return clean.startswith("（") and clean.endswith("）") or clean in ALL_BABY_REACTIONS


def classify_baby_reply_mood(request: GenerateDiaryRequest, result: GenerateDiaryResponse) -> str:
    combined = " ".join(
        part
        for part in [
            combined_text(request),
            result.cardSummary,
            result.cardEmoji,
            result.babyText,
        ]
        if part
    )
    if has_any(combined, "伤心", "难过", "委屈", "害怕", "失落", "孤单", "不开心", "哭", "没有", "忘了", "冷落"):
        return "sad"
    if has_any(combined, "胎动", "动了一下", "踢"):
        return "movement"
    if has_any(combined, "累", "疲惫", "困", "撑不住", "沉", "难受"):
        return "tired"
    if has_any(combined, "开心", "高兴", "快乐", "幸福", "可爱", "喜欢", "期待", "真好", "陪在我身边"):
        return "happy"
    return "neutral"


def baby_reply_bucket(request: GenerateDiaryRequest) -> int:
    key = "|".join(
        [
            request.dateId,
            request.inputSource,
            request.text,
            request.voiceText,
        ],
    )
    digest = hashlib.sha256(key.encode("utf-8")).hexdigest()
    return int(digest[:8], 16) % 100


HAPPY_BABY_REACTIONS = (
    "😊",
    "✨",
    "🌷",
    "🥰",
    "🫧",
    "💕",
    "💫",
    "🌟",
    "💛",
    "🌸",
    "（噗噜噗噜）",
    "（小手挥挥）",
    "（咕噜咕噜）",
    "（开心冒泡）",
    "（蹬蹬腿）",
    "（伸伸小手）",
    "（翻了个身）",
    "（轻轻转圈）",
    "（晃晃小脚）",
    "（眨眨眼）",
    "（轻轻点点）",
    "（咕噜冒泡）",
    "（软软点头）",
    "（小脚踢踢）",
    "（悄悄拍手）",
    "（转了个圈）",
    "（蹭蹭妈妈）",
    "（小手拍拍）",
    "（肚肚冒泡）",
    "（伸个小懒）",
    "（轻轻摆摆）",
    "（翻身贴贴）",
    "（小脚晃晃）",
    "（呼噜一下）",
    "（冒个小泡）",
    "（软软贴住）",
    "（开心蹭蹭）",
    "（小手贴贴）",
    "（咕噜翻身）",
    "（贴贴肚肚）",
    "（轻轻踢踢）",
    "（甜甜打盹）",
)

NEUTRAL_BABY_REACTIONS = (
    "✨",
    "🌙",
    "🫧",
    "🤍",
    "☁️",
    "🌿",
    "💫",
    "🌼",
    "🩷",
    "💛",
    "（噗噜噗噜）",
    "（小手挥挥）",
    "（咕噜一下）",
    "（慢慢打盹）",
    "（安静漂浮）",
    "（轻轻翻身）",
    "（眨眨眼）",
    "（慢慢贴贴）",
    "（软软呼吸）",
    "（悄悄陪着）",
    "（睡成一团）",
    "（小脚收好）",
    "（伸伸小手）",
    "（翻了个身）",
    "（呼噜呼噜）",
    "（肚肚贴贴）",
    "（小手贴贴）",
    "（轻轻靠着）",
    "（软软打盹）",
    "（慢慢漂漂）",
    "（咕噜冒泡）",
    "（小脚晃晃）",
    "（安静贴住）",
    "（翻身贴贴）",
    "（呼呼睡着）",
    "（轻轻动动）",
    "（贴住肚肚）",
    "（慢慢眨眼）",
    "（小手握握）",
    "（软软靠着）",
    "（安静呼吸）",
    "（贴着听听）",
)

SAD_BABY_REACTIONS = (
    "🤍",
    "🫶",
    "☁️",
    "🌙",
    "🫧",
    "💛",
    "🌧️",
    "🩷",
    "💫",
    "✨",
    "（轻轻贴贴）",
    "（小手握握）",
    "（蹭蹭妈妈）",
    "（安静陪着）",
    "（靠近一点）",
    "（软软贴住）",
    "（小手贴贴）",
    "（轻轻呼呼）",
    "（贴住肚肚）",
    "（小脚停停）",
    "（慢慢靠近）",
    "（抱抱肚肚）",
    "（翻身贴贴）",
    "（悄悄贴住）",
    "（轻轻蹭蹭）",
    "（呼噜呼噜）",
    "（陪你一会）",
    "（安静呼吸）",
    "（小手握住）",
    "（软软靠着）",
    "（慢慢打盹）",
    "（肚肚贴贴）",
    "（小脚收好）",
    "（轻轻贴住）",
    "（睡成一团）",
    "（靠近妈妈）",
    "（小手抱抱）",
    "（呼呼陪着）",
    "（软软陪着）",
    "（小脚停一停）",
)

TIRED_BABY_REACTIONS = (
    "☁️",
    "🤍",
    "🌙",
    "🫧",
    "✨",
    "💤",
    "🌿",
    "💛",
    "🩷",
    "💫",
    "（轻轻蹭蹭）",
    "（慢慢呼呼）",
    "（呼噜呼噜）",
    "（陪你歇歇）",
    "（软软打盹）",
    "（小脚收好）",
    "（慢慢翻身）",
    "（睡成一团）",
    "（小手贴贴）",
    "（软软靠着）",
    "（安静打盹）",
    "（肚肚抱抱）",
    "（轻轻摇摇）",
    "（呼呼睡着）",
    "（贴住肚肚）",
    "（翻身贴贴）",
    "（小手握握）",
    "（慢慢眨眼）",
    "（轻轻呼吸）",
    "（伸个小懒）",
    "（小脚缩缩）",
    "（安静靠着）",
    "（肚肚贴贴）",
    "（咕噜一下）",
    "（软软贴住）",
    "（慢慢漂漂）",
    "（小手收好）",
    "（轻轻打盹）",
    "（呼噜一下）",
    "（抱抱肚肚）",
    "（一起慢慢）",
)

MOVEMENT_BABY_REACTIONS = (
    "✨",
    "🫧",
    "😊",
    "🌷",
    "🤍",
    "💫",
    "🌟",
    "💕",
    "💛",
    "🥰",
    "（小脚踢踢）",
    "（噗噜噗噜）",
    "（小手挥挥）",
    "（蹬蹬腿）",
    "（轻轻冒泡）",
    "（转个小圈）",
    "（翻了个身）",
    "（肚肚敲敲）",
    "（软软动动）",
    "（小脚晃晃）",
    "（伸伸小手）",
    "（咕噜一下）",
    "（轻轻踢踢）",
    "（冒个小泡）",
    "（转了个圈）",
    "（小手拍拍）",
    "（翻身贴贴）",
    "（肚肚冒泡）",
    "（伸个小懒）",
    "（蹬了蹬腿）",
    "（悄悄回应）",
    "（小脚点点）",
    "（咕噜翻身）",
    "（轻轻摆摆）",
    "（软软伸手）",
    "（贴贴肚肚）",
    "（踢了两下）",
    "（晃晃小脚）",
    "（小手贴贴）",
    "（翻身冒泡）",
    "（轻轻点点）",
)

ALL_BABY_REACTIONS = (
    *HAPPY_BABY_REACTIONS,
    *NEUTRAL_BABY_REACTIONS,
    *SAD_BABY_REACTIONS,
    *TIRED_BABY_REACTIONS,
    *MOVEMENT_BABY_REACTIONS,
)

GENERIC_BABY_TEXTS = (
    "我在这里听着呢。",
)
