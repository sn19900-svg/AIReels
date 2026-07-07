package com.nabil.aireels.domain.repository

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.CaptionOverlay
import com.nabil.aireels.domain.model.Clip

interface VideoRepository {
    suspend fun trimClip(inputPath: String, startMs: Long, endMs: Long, outputPath: String): AppResult<String>
    suspend fun mergeClips(clips: List<Clip>, outputPath: String): AppResult<String>
    suspend fun addAudioTrack(videoPath: String, audioPath: String, outputPath: String): AppResult<String>
    suspend fun extractAudio(videoPath: String, outputPath: String): AppResult<String>
    suspend fun createKenBurnsSegment(
        imagePath: String,
        durationSeconds: Double,
        outputPath: String,
        width: Int,
        height: Int
    ): AppResult<String>
    suspend fun overlayCaptionImages(
        videoPath: String,
        captionOverlays: List<CaptionOverlay>,
        outputPath: String
    ): AppResult<String>
}
