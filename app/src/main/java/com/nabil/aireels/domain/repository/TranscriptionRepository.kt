package com.nabil.aireels.domain.repository

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.TranscriptSegment

interface TranscriptionRepository {
    suspend fun transcribeAudio(pcmAudioPath: String): AppResult<List<TranscriptSegment>>
}
