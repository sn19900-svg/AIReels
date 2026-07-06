package com.nabil.aireels.data.repository

import com.nabil.aireels.BuildConfig
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.core.util.Constants
import com.nabil.aireels.data.remote.gemini.GeminiApiService
import com.nabil.aireels.data.remote.gemini.GeminiContent
import com.nabil.aireels.data.remote.gemini.GeminiPart
import com.nabil.aireels.data.remote.gemini.GeminiRequest
import com.nabil.aireels.domain.model.ScriptSuggestion
import com.nabil.aireels.domain.repository.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class GeminiRepositoryImpl @Inject constructor(
    private val geminiApiService: GeminiApiService
) : GeminiRepository {

    override suspend fun generateReelScript(
        topic: String,
        tone: String,
        durationSeconds: Int
    ): AppResult<ScriptSuggestion> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                return@withContext AppResult.Error(
                    "مفتاح Gemini API غير موجود. أضفه في local.properties أو GitHub Secrets"
                )
            }

            val prompt = buildPrompt(topic, tone, durationSeconds)
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
            )

            val response = geminiApiService.generateContent(
                model = Constants.GEMINI_MODEL,
                apiKey = apiKey,
                request = request
            )

            val rawText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: return@withContext AppResult.Error("لم يتم استلام رد من Gemini")

            val cleanedJson = extractJson(rawText)
            val json = JSONObject(cleanedJson)

            val captionsArray = json.optJSONArray("captions")
            val captions = mutableListOf<String>()
            if (captionsArray != null) {
                for (i in 0 until captionsArray.length()) {
                    captions.add(captionsArray.getString(i))
                }
            }

            val hashtagsArray = json.optJSONArray("hashtags")
            val hashtags = mutableListOf<String>()
            if (hashtagsArray != null) {
                for (i in 0 until hashtagsArray.length()) {
                    hashtags.add(hashtagsArray.getString(i))
                }
            }

            val suggestion = ScriptSuggestion(
                hook = json.optString("hook", ""),
                fullScript = json.optString("full_script", ""),
                captions = captions,
                hashtags = hashtags
            )

            AppResult.Success(suggestion)
        } catch (e: Exception) {
            AppResult.Error("فشل توليد النص: ${e.message}", e)
        }
    }

    private fun buildPrompt(topic: String, tone: String, durationSeconds: Int): String {
        return """
            أنت كاتب محتوى محترف لمنصات الفيديو القصيرة (ريلز/شورتس).
            الموضوع: $topic
            الأسلوب المطلوب: $tone
            مدة الفيديو المستهدفة بالثواني: $durationSeconds

            أعطني الناتج بصيغة JSON فقط وبدون أي نص إضافي قبله أو بعده، بالمخطط التالي بالضبط:
            {
              "hook": "جملة افتتاحية قوية لجذب المشاهد في أول 3 ثواني",
              "full_script": "النص الكامل للفيديو مقسم بفقرات قصيرة",
              "captions": ["سطر ترجمة 1", "سطر ترجمة 2", "سطر ترجمة 3"],
              "hashtags": ["#وسم1", "#وسم2", "#وسم3"]
            }
        """.trimIndent()
    }

    private fun extractJson(rawText: String): String {
        val start = rawText.indexOf('{')
        val end = rawText.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) {
            rawText.substring(start, end + 1)
        } else {
            rawText
        }
    }
}
