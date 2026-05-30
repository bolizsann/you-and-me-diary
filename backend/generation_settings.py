DEFAULT_MODEL = "gemini-2.5-flash-lite"
GEMMA_TIMEOUT_SECONDS = 45
GEMMA_DEFAULT_MAX_OUTPUT_TOKENS = 640
GEMMA_PRESERVE_MAX_OUTPUT_TOKENS = 384


def max_output_tokens_for(diary_text_mode: str) -> int:
    if diary_text_mode == "preserve":
        return GEMMA_PRESERVE_MAX_OUTPUT_TOKENS
    return GEMMA_DEFAULT_MAX_OUTPUT_TOKENS
