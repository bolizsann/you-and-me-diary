package com.youandme.diary.data.generation

import android.os.Debug
import android.os.SystemClock
import android.util.Log

internal object DiaryBenchmarkLogger {
    private const val TAG = "YmdBench"

    fun log(
        operation: String,
        mode: String,
        status: String,
        startedAtMs: Long,
        fields: Map<String, Any?> = emptyMap(),
    ) {
        val elapsedMs = SystemClock.elapsedRealtime() - startedAtMs
        val allFields = linkedMapOf<String, Any?>(
            "op" to operation,
            "mode" to mode,
            "status" to status,
            "elapsedMs" to elapsedMs,
        )
        allFields.putAll(fields)
        allFields.putAll(memoryFields())
        Log.i(TAG, allFields.toBenchmarkLine())
    }

    private fun memoryFields(): Map<String, Any> {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        val runtime = Runtime.getRuntime()
        return linkedMapOf(
            "totalPssKb" to info.totalPss,
            "dalvikPssKb" to info.dalvikPss,
            "nativePssKb" to info.nativePss,
            "otherPssKb" to info.otherPss,
            "graphicsKb" to info.getMemoryStat("summary.graphics").toIntOrMinusOne(),
            "javaHeapKb" to info.getMemoryStat("summary.java-heap").toIntOrMinusOne(),
            "nativeHeapKb" to (Debug.getNativeHeapAllocatedSize() / 1024L),
            "runtimeUsedKb" to ((runtime.totalMemory() - runtime.freeMemory()) / 1024L),
            "runtimeMaxKb" to (runtime.maxMemory() / 1024L),
        )
    }

    private fun Map<String, Any?>.toBenchmarkLine(): String =
        entries.joinToString(separator = " ") { (key, value) ->
            "$key=${value.toBenchmarkValue()}"
        }

    private fun Any?.toBenchmarkValue(): String =
        when (this) {
            null -> "null"
            is Boolean, is Number -> toString()
            else -> toString()
                .replace("\\", "\\\\")
                .replace(" ", "_")
                .replace("\n", "_")
                .take(120)
        }

    private fun String?.toIntOrMinusOne(): Int =
        this?.toIntOrNull() ?: -1
}
