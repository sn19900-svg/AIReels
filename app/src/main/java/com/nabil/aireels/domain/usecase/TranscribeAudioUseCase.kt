package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.TranscriptSegment
import com.nabil.aireels.domain.repository.TranscriptionRepository
import javax.inject.Inject

class TranscribeAudioUseCase @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository
) {
    suspend operator fun invoke(pcmAudioPath: String): AppResult<List<TranscriptSegment>> {
        return transcriptionRepository.transcribeAudio(pcmAudioPath)
    }
}
