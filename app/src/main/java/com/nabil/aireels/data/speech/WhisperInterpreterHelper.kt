package com.nabil.aireels.data.speech

import android.content.Context
import com.nabil.aireels.core.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperInterpreterHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var vocabMap: Map<Int, String>? = null
    private val melProcessor = MelSpectrogramProcessor()

    private fun ensureLoaded() {
        if (interpreter == null) {
            interpreter = Interpreter(loadModelFile(Constants.WHISPER_MODEL_ASSET))
        }
        if (vocabMap == null) {
            vocabMap = loadVocab(Constants.WHISPER_VOCAB_ASSET)
        }
    }

    fun transcribe(audioSamples: FloatArray): String {
        ensureLoaded()
        val currentInterpreter = interpreter
            ?: throw IllegalStateException("لم يتم تحميل نموذج Whisper بنجاح")

        val melFrames = melProcessor.process(audioSamples)
        val nMels = 80
        val nFrames = melFrames.size

        val inputBuffer = Array(1) { Array(nMels) { FloatArray(nFrames) } }
        for (t in 0 until nFrames) {
            for (m in 0 until nMels) {
                inputBuffer[0][m][t] = melFrames[t][m]
            }
        }

        val maxTokens = 224
        val outputTokens = Array(1) { IntArray(maxTokens) }

        currentInterpreter.run(inputBuffer, outputTokens)

        return decodeTokens(outputTokens[0])
    }

    private fun decodeTokens(tokenIds: IntArray): String {
        val map = vocabMap ?: return ""
        val builder = StringBuilder()
        for (id in tokenIds) {
            if (id <= 0) break
            val token = map[id] ?: continue
            if (token.startsWith("<|") && token.endsWith("|>")) continue
            builder.append(token.replace("\u0120", " "))
        }
        return builder.toString().trim()
    }

    private fun loadModelFile(assetName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(assetName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadVocab(assetName: String): Map<Int, String> {
        val jsonText = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val json = JSONObject(jsonText)
        val map = mutableMapOf<Int, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key.toInt()] = json.getString(key)
        }
        return map
    }
}
