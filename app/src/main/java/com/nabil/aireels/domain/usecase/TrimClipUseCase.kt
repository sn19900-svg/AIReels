package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.repository.VideoRepository
import javax.inject.Inject

class TrimClipUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(inputPath: String, startMs: Long, endMs: Long, outputPath: String): AppResult<String> {
        if (endMs <= startMs) {
            return AppResult.Error("زمن النهاية يجب أن يكون أكبر من زمن البداية")
        }
        return videoRepository.trimClip(inputPath, startMs, endMs, outputPath)
    }
}
