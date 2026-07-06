package com.nabil.aireels.domain.repository

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.ScriptSuggestion

interface GeminiRepository {
    suspend fun generateReelScript(topic: String, tone: String, durationSeconds: Int): AppResult<ScriptSuggestion>
}
