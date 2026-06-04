package com.youandme.diary.data.localai

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.SamplerConfig
import com.youandme.diary.data.local.GeneratedDiaryDraft
import com.youandme.diary.data.voice.toWavBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class LocalGemmaClient(
    context: Context,
    private val modelFileName: String = MODEL_FILE_NAME,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val engineCacheDir = File(appContext.cacheDir, "local_gemma_engine").apply { mkdirs() }
    private val engineMutex = Mutex()
    private var engine: Engine? = null
    private var activeBackend: LocalGemmaBackend? = null

    suspend fun generate(request: GenerateDiaryLocalRequest): GeneratedDiaryDraft? =
        generateWithMetrics(request)?.draft

    suspend fun warmUp(): LocalGemmaWarmUpResult? =
        withContext(Dispatchers.IO) {
            val totalStartedAt = SystemClock.elapsedRealtime()
            val modelFile = modelFile()
            if (!modelFile.exists()) {
                return@withContext null
            }
            runCatching {
                val initStartedAt = SystemClock.elapsedRealtime()
                val backend = ensureEngine(modelFile)
                val initMs = SystemClock.elapsedRealtime() - initStartedAt
                val totalMs = SystemClock.elapsedRealtime() - totalStartedAt
                LocalGemmaWarmUpResult(
                    backend = backend.id,
                    initMs = initMs,
                    totalMs = totalMs,
                )
            }.onFailure {
                resetEngine()
            }.getOrNull()
        }

    suspend fun generateWithMetrics(request: GenerateDiaryLocalRequest): LocalGemmaGenerationResult? =
        withContext(Dispatchers.IO) {
            val totalStartedAt = SystemClock.elapsedRealtime()
            val modelFile = modelFile()
            if (!modelFile.exists()) {
                Log.w(TAG, "Local model missing: ${modelFile.absolutePath}")
                return@withContext null
            }
            runCatching {
                val initStartedAt = SystemClock.elapsedRealtime()
                val backend = ensureEngine(modelFile)
                val initMs = SystemClock.elapsedRealtime() - initStartedAt
                val inferenceStartedAt = SystemClock.elapsedRealtime()
                val responseText = runInference(
                    prompt = buildPrompt(request),
                    imagePath = request.imagePath,
                )
                val inferenceMs = SystemClock.elapsedRealtime() - inferenceStartedAt
                val totalMs = SystemClock.elapsedRealtime() - totalStartedAt
                val source = "local-gemma-${backend.id}"
                val draft = responseText
                    .toGeneratedDiaryDraft(source = source)
                    .applyLocalGenerationPolicy(request)
                LocalGemmaGenerationResult(
                    draft = draft,
                    backend = backend.id,
                    initMs = initMs,
                    inferenceMs = inferenceMs,
                    totalMs = totalMs,
                    rawLength = responseText.length,
                )
            }.onFailure { error ->
                Log.w(TAG, "Local Gemma generation failed: ${error.javaClass.simpleName}: ${error.message}")
                resetEngine()
            }.getOrNull()
        }

    fun modelPath(): String = modelFile().absolutePath

    override fun close() {
        resetEngine()
    }

    private fun modelFile(): File =
        File(appContext.getExternalFilesDir("models") ?: File(appContext.filesDir, "models"), modelFileName)

    private suspend fun ensureEngine(modelFile: File): LocalGemmaBackend =
        engineMutex.withLock {
            engine?.takeIf { it.isInitialized() }?.let {
                return@withLock activeBackend ?: LocalGemmaBackend.Cpu
            }

            resetEngine()
            val gpuResult = initializeEngine(modelFile = modelFile, backend = LocalGemmaBackend.Gpu)
            if (gpuResult != null) {
                engine = gpuResult
                activeBackend = LocalGemmaBackend.Gpu
                return@withLock LocalGemmaBackend.Gpu
            }

            val cpuResult = initializeEngine(modelFile = modelFile, backend = LocalGemmaBackend.Cpu)
            if (cpuResult != null) {
                engine = cpuResult
                activeBackend = LocalGemmaBackend.Cpu
                return@withLock LocalGemmaBackend.Cpu
            }

            error("Unable to initialize LiteRT-LM with GPU or CPU backend.")
        }

    private fun initializeEngine(modelFile: File, backend: LocalGemmaBackend): Engine? {
        val litertBackend = backend.toLiteRtBackend()
        return runCatching {
            configureExperimentalFlags()
            Engine(
                EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = litertBackend,
                    visionBackend = litertBackend,
                    audioBackend = Backend.CPU(),
                    maxNumTokens = MAX_NUM_TOKENS,
                    maxNumImages = MAX_NUM_IMAGES,
                    cacheDir = engineCacheDir.absolutePath,
                ),
            ).also { it.initialize() }
        }.onFailure { error ->
            Log.w(TAG, "Failed to initialize ${backend.id}: ${error.javaClass.simpleName}: ${error.message}")
        }.getOrNull()
    }

    @OptIn(ExperimentalApi::class)
    private fun configureExperimentalFlags() {
        ExperimentalFlags.enableSpeculativeDecoding = true
    }

    private fun runInference(prompt: String, imagePath: String?): String {
        val currentEngine = checkNotNull(engine) { "Local Gemma engine is not initialized." }
        val conversation = currentEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(SYSTEM_INSTRUCTION),
                samplerConfig = SamplerConfig(
                    topK = 20,
                    topP = 0.9,
                    temperature = 0.35,
                    seed = 7,
                ),
            ),
        )
        return try {
            val contents = if (imagePath.isNullOrBlank()) {
                Contents.of(Content.Text(prompt))
            } else {
                Contents.of(Content.ImageFile(imagePath), Content.Text(prompt))
            }
            conversation.sendMessage(contents).toString().trim()
        } finally {
            conversation.close()
        }
    }

    suspend fun transcribeAudio(audioBytes: ByteArray): LocalGemmaVoiceTranscriptionResult? =
        withContext(Dispatchers.IO) {
            val totalStartedAt = SystemClock.elapsedRealtime()
            val modelFile = modelFile()
            if (!modelFile.exists()) {
                Log.w(TAG, "Local model missing: ${modelFile.absolutePath}")
                return@withContext null
            }
            if (audioBytes.isEmpty()) {
                Log.w(TAG, "Local transcription audio bytes are empty")
                return@withContext null
            }
            runCatching {
                val initStartedAt = SystemClock.elapsedRealtime()
                val backend = ensureEngine(modelFile)
                val initMs = SystemClock.elapsedRealtime() - initStartedAt
                val inferenceStartedAt = SystemClock.elapsedRealtime()
                val transcript = runAudioTranscription(audioBytes = audioBytes)
                val inferenceMs = SystemClock.elapsedRealtime() - inferenceStartedAt
                LocalGemmaVoiceTranscriptionResult(
                    transcript = cleanLocalTranscript(transcript),
                    backend = backend.id,
                    initMs = initMs,
                    inferenceMs = inferenceMs,
                    totalMs = SystemClock.elapsedRealtime() - totalStartedAt,
                    rawLength = transcript.length,
                )
            }.onFailure { error ->
                Log.w(TAG, "Local voice transcription failed: ${error.javaClass.simpleName}: ${error.message}")
                resetEngine()
            }.getOrNull()
        }

    private fun runAudioTranscription(audioBytes: ByteArray): String {
        val currentEngine = checkNotNull(engine) { "Local Gemma engine is not initialized." }
        val wavBytes = audioBytes.toWavBytes()
        Log.i(TAG, "Local voice transcription payload rawBytes=${audioBytes.size} wavBytes=${wavBytes.size}")
        val conversation = currentEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of("你是一个语音转写助手。只输出用户说出的内容，不解释、不总结。"),
                samplerConfig = SamplerConfig(
                    topK = 10,
                    topP = 0.8,
                    temperature = 0.1,
                    seed = 11,
                ),
            ),
        )
        return try {
            conversation.sendMessage(
                Contents.of(
                    Content.AudioBytes(wavBytes),
                    Content.Text("请把这段音频转写成简体中文。只输出转写文本。"),
                ),
            ).toString().trim()
        } finally {
            conversation.close()
        }
    }

    private fun resetEngine() {
        runCatching { engine?.close() }
        engine = null
        activeBackend = null
    }
}

data class GenerateDiaryLocalRequest(
    val text: String,
    val voiceText: String,
    val inputSource: String,
    val diaryTextMode: String,
    val dateId: String,
    val dateLabel: String,
    val currentTitle: String,
    val isFirstRecordForDay: Boolean,
    val username: String,
    val estimatedDueDate: String?,
    val imagePath: String?,
    val dominantColor: String?,
)

data class LocalGemmaGenerationResult(
    val draft: GeneratedDiaryDraft,
    val backend: String,
    val initMs: Long,
    val inferenceMs: Long,
    val totalMs: Long,
    val rawLength: Int,
)

data class LocalGemmaWarmUpResult(
    val backend: String,
    val initMs: Long,
    val totalMs: Long,
)

data class LocalGemmaVoiceTranscriptionResult(
    val transcript: String,
    val backend: String,
    val initMs: Long,
    val inferenceMs: Long,
    val totalMs: Long,
    val rawLength: Int,
)

private enum class LocalGemmaBackend(val id: String) {
    Gpu("gpu"),
    Cpu("cpu");

    fun toLiteRtBackend(): Backend =
        when (this) {
            Gpu -> Backend.GPU()
            Cpu -> Backend.CPU(4)
        }
}

internal fun buildPrompt(request: GenerateDiaryLocalRequest): String =
    """
    你是 You & Me Diary 的孕期私密日记助手。温柔、克制、像日记；不诊断、不治疗、不建议药物。只输出 JSON。

    输入：日期=${request.dateLabel}；模式=${request.diaryTextMode}；有图片=${!request.imagePath.isNullOrBlank()}；图片主色=${request.dominantColor ?: "未知"}。
    用户手写：${request.text.ifBlank { "无" }}
    语音转写：${request.voiceText.ifBlank { "无" }}
    合并输入：${request.combinedText().ifBlank { "无" }}

    规则：preserve 时 diaryText 原样返回合并输入，不润色不扩写。polish 以合并输入为准，只整理断句和轻微错字，保留第一人称和用户额外添加的 emoji。generate 时结合图片生成 30-50 字正文。有图片只描述可见场景、物体、颜色和氛围，不编造。cardSummary 是图卡文字短句，不是 diaryText，最多 8 个中文字符；不要复刻、截取或保留整段输入，不要包含 emoji；只从用户原文中提炼最贴近当下的核心情绪、愿望或身体感受。cardEmoji 是模型识别出的图卡情绪 emoji，最多一个；普通记录可为空。babyText 是候选宝宝说，12-36 字，可为空，开心/普通记录要克制。高风险孕期描述只写 safetyNote，不写进 babyText。

    JSON 字段：
    {
      "titleSuggestion": "最多12个中文字符",
      "diaryText": "preserve时原样文本，否则30-50字",
      "cardSummary": "最多8个中文字符或空字符串",
      "cardEmoji": "一个emoji或空字符串",
      "babyText": "候选宝宝说或空字符串",
      "safetyNote": "安全提醒或空字符串"
    }
    """.trimIndent()

private fun String.toGeneratedDiaryDraft(source: String): GeneratedDiaryDraft {
    val compact = trim()
    val json = compact.extractJsonObject()
    if (json != null) {
        runCatching {
            val body = JSONObject(json)
            return GeneratedDiaryDraft(
                titleSuggestion = body.optString("titleSuggestion"),
                diaryText = body.optString("diaryText"),
                cardSummary = body.optString("cardSummary"),
                cardEmoji = body.optString("cardEmoji"),
                babyText = body.optString("babyText"),
                safetyNote = body.optString("safetyNote"),
                source = source,
            )
        }.onFailure { error ->
            Log.w(TAG, "Local Gemma JSON parse failed: ${error.javaClass.simpleName}: ${error.message}")
        }
    }

    val title = compact.extractJsonStringField("titleSuggestion")
    val diaryText = compact.extractJsonStringField("diaryText")
    val cardSummary = compact.extractJsonStringField("cardSummary")
    return GeneratedDiaryDraft(
        titleSuggestion = title.ifBlank { "今天也留一页" },
        diaryText = diaryText.ifBlank { compact.take(MAX_FALLBACK_TEXT_LENGTH) },
        cardSummary = cardSummary,
        cardEmoji = compact.extractJsonStringField("cardEmoji"),
        babyText = compact.extractJsonStringField("babyText"),
        safetyNote = compact.extractJsonStringField("safetyNote"),
        source = source,
    )
}

private fun String.extractJsonObject(): String? {
    val start = indexOf('{')
    val end = lastIndexOf('}')
    return if (start >= 0 && end > start) substring(start, end + 1) else null
}

private fun String.extractJsonStringField(key: String): String =
    Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.replace("\\\"", "\"")
        ?.replace("\\n", "\n")
        .orEmpty()

private fun GenerateDiaryLocalRequest.combinedText(): String =
    listOf(text, voiceText).filter { it.isNotBlank() }.joinToString(separator = " ")

private fun cleanLocalTranscript(text: String): String {
    var clean = text.trim()
    val json = clean.extractJsonObject()
    if (json != null) {
        runCatching {
            val body = JSONObject(json)
            clean = body.optString("transcript").ifBlank { body.optString("text") }.ifBlank { clean }
        }
    }
    clean = clean
        .removePrefix("转写：")
        .removePrefix("转写文本：")
        .removePrefix("文本：")
        .trim()
    if (clean.length >= 2 && clean.first() in setOf('"', '\'') && clean.last() == clean.first()) {
        clean = clean.substring(1, clean.lastIndex).trim()
    }
    return clean
}

private const val TAG = "LocalGemma"
private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
private const val MAX_NUM_TOKENS = 768
private const val MAX_NUM_IMAGES = 1
private const val MAX_FALLBACK_TEXT_LENGTH = 160
private const val SYSTEM_INSTRUCTION =
    "你是一个私密孕期陪伴日记助手。你的任务是温柔理解用户的图片和文字，生成克制、具体、非医疗的中文日记内容。"
