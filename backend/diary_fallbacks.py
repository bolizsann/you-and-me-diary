import unicodedata

from schemas import GenerateDiaryRequest


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


def select_card_summary(request: GenerateDiaryRequest, generated_summary: str) -> str:
    clean = strip_symbols(generated_summary.strip()).strip()
    if clean and len(clean) <= 8:
        return clean
    return fallback_card_summary(request)


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


def select_card_emoji(request: GenerateDiaryRequest, generated_emoji: str) -> str:
    clean = first_symbol(generated_emoji.strip())
    if clean:
        return clean
    return fallback_card_emoji(request)


def fallback_baby_text(request: GenerateDiaryRequest) -> str:
    combined = combined_text(request)
    if has_any(combined, "胎动", "动了一下", "踢"):
        return "妈妈，我也在轻轻回应你。"
    if has_any(combined, "累", "疲惫", "困", "撑不住"):
        return "妈妈辛苦啦，我陪你慢慢来。"
    if has_any(combined, "伤心", "难过", "委屈", "害怕", "失落", "孤单", "不开心", "哭"):
        return "我在这里陪着你。"
    if has_any(combined, "开心", "高兴", "快乐", "幸福", "可爱", "喜欢", "真好"):
        return "我也跟着开心了一下。"
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


def select_card_emoji_from_fields(
    request: GenerateDiaryRequest,
    generated_summary: str,
    generated_emoji: str,
) -> str:
    clean = first_symbol(generated_emoji.strip()) or first_symbol(generated_summary.strip())
    if clean:
        return clean
    return fallback_card_emoji(request)


def first_symbol(text: str) -> str:
    for index, char in enumerate(text):
        if unicodedata.category(char) == "So":
            end = index + 1
            while end < len(text) and unicodedata.category(text[end]) in {"Mn", "Sk"}:
                end += 1
            return text[index:end]
    return ""


def strip_symbols(text: str) -> str:
    return "".join(char for char in text if unicodedata.category(char) != "So")
