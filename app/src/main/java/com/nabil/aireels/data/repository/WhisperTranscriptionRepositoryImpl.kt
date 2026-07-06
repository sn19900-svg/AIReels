package com.nabil.aireels.data.repository

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.speech.WhisperInterpreterHelper
import com.nabil.aireels.domain.model.TranscriptSegment
import com.nabil.aireels.domain.repository.TranscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject

class WhisperTranscriptionRepositoryImpl @Inject constructor(
    private val whisperInterpreterHelper: WhisperInterpreterHelper
) : TranscriptionRepository {

    private val sampleRate = 16000
    private val chunkDurationSeconds = 30
    private val chunkSizeSamples = sampleRate * chunkDurationSeconds

    override suspend fun transcribeAudio(pcmAudioPath: String): AppResult<List<TranscriptSegment>> =
        withContext(Dispatchers.Default) {
            try {
                val file = File(pcmAudioPath)
                if (!file.exists()) {
                    return@withContext AppResult.Error("ملف الصوت غير موجود: $pcmAudioPath")
                }

                val samples = readWavAsFloatArray(file)
                val segments = mutableListOf<TranscriptSegment>()

                var offset = 0
                while (offset < samples.size) {
                    val end = minOf(offset + chunkSizeSamples, samples.size)
                    val chunk = samples.copyOfRange(offset, end)
                    val text = whisperInterpreterHelper.transcribe(chunk)

                    if (text.isNotBlank()) {
                        val startMs = (offset.toLong() * 1000L) / sampleRate
                        val endMs = (end.toLong() * 1000L) / sampleRate
                        segments.add(TranscriptSegment(text = text, startMs = startMs, endMs = endMs))
                    }
                    offset += chunkSizeSamples
                }

                AppResult.Success(segments)
            } catch (e: Exception) {
                AppResult.Error("فشل تحويل الصوت إلى نص: ${e.message}", e)
            }
        }

    private fun readWavAsFloatArray(file: File): FloatArray {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(44)
            raf.readFully(header)
            val dataSize = raf.length().toInt() - 44
            val pcmBytes = ByteArray(dataSize)
            raf.readFully(pcmBytes)

            val sampleCount = dataSize / 2
            val floatSamples = FloatArray(sampleCount)
            for (i in 0 until sampleCount) {
                val low = pcmBytes[i * 2].toInt() and 0xFF
                val high = pcmBytes[i * 2 + 1].toInt()
                val sampleValue = (high shl 8) or low
                floatSamples[i] = sampleValue.toShort().toFloat() / 32768.0f
            }
            return floatSamples
        }
    }
}
