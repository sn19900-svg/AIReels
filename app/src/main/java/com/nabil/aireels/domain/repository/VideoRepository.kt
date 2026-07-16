package com.nabil.aireels.domain.repository

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.CaptionOverlay
import com.nabil.aireels.domain.model.Clip
import com.nabil.aireels.domain.model.KenBurnsMotionStyle

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
        height: Int,
        motionStyle: KenBurnsMotionStyle
    ): AppResult<String>
    suspend fun overlayCaptionImages(
        videoPath: String,
        captionOverlays: List<CaptionOverlay>,
        outputPath: String
    ): AppResult<String>
    suspend fun concatWithCrossfade(
        segments: List<Pair<String, Double>>,
        transitionSeconds: Double,
        transitionNames: List<String>,
        outputPath: String
    ): AppResult<String>
    suspend fun prepareStockVideoSegment(
        videoPath: String,
        durationSeconds: Double,
        outputPath: String,
        width: Int,
        height: Int
    ): AppResult<String>
    suspend fun createHeroImageSegment(
        imagePath: String,
        durationSeconds: Double,
        outputPath: String,
        width: Int,
        height: Int
    ): AppResult<String>
    suspend fun applyColorGrade(
        videoPath: String,
        colorGrade: String,
        outputPath: String
    ): AppResult<String>
}
