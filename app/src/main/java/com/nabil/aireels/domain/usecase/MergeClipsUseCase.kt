package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.Clip
import com.nabil.aireels.domain.repository.VideoRepository
import javax.inject.Inject

class MergeClipsUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(clips: List<Clip>, outputPath: String): AppResult<String> {
        if (clips.isEmpty()) {
            return AppResult.Error("لا توجد مقاطع فيديو لدمجها")
        }
        return videoRepository.mergeClips(clips, outputPath)
    }
}
