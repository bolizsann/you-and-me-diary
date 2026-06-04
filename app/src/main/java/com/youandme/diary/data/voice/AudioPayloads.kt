package com.youandme.diary.data.voice

import java.io.ByteArrayOutputStream

internal const val VOICE_SAMPLE_RATE = 16_000

internal fun ByteArray.toWavBytes(
    sampleRate: Int = VOICE_SAMPLE_RATE,
    channels: Short = 1,
    bitsPerSample: Short = 16,
): ByteArray {
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val dataSize = size
    val out = ByteArrayOutputStream(44 + dataSize)
    out.writeAscii("RIFF")
    out.writeIntLE(36 + dataSize)
    out.writeAscii("WAVE")
    out.writeAscii("fmt ")
    out.writeIntLE(16)
    out.writeShortLE(1)
    out.writeShortLE(channels.toInt())
    out.writeIntLE(sampleRate)
    out.writeIntLE(byteRate)
    out.writeShortLE((channels * bitsPerSample / 8).toInt())
    out.writeShortLE(bitsPerSample.toInt())
    out.writeAscii("data")
    out.writeIntLE(dataSize)
    out.write(this)
    return out.toByteArray()
}

private fun ByteArrayOutputStream.writeAscii(value: String) {
    write(value.toByteArray(Charsets.US_ASCII))
}

private fun ByteArrayOutputStream.writeIntLE(value: Int) {
    write(value and 0xff)
    write(value shr 8 and 0xff)
    write(value shr 16 and 0xff)
    write(value shr 24 and 0xff)
}

private fun ByteArrayOutputStream.writeShortLE(value: Int) {
    write(value and 0xff)
    write(value shr 8 and 0xff)
}
