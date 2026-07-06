package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.ScriptSuggestion
import com.nabil.aireels.domain.repository.GeminiRepository
import javax.inject.Inject

class GenerateReelScriptUseCase @Inject constructor(
    private val geminiRepository: GeminiRepository
) {
    suspend operator fun invoke(topic: String, tone: String, durationSeconds: Int): AppResult<ScriptSuggestion> {
        if (topic.isBlank()) {
            return AppResult.Error("الرجاء إدخال موضوع الريلز أولاً")
        }
        return geminiRepository.generateReelScript(topic, tone, durationSeconds)
    }
}
