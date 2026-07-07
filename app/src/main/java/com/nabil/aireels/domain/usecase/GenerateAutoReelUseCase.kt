package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.render.CaptionRenderer
import com.nabil.aireels.data.repository.PexelsRepository
import com.nabil.aireels.domain.model.CaptionOverlay
import com.nabil.aireels.domain.model.ScriptSuggestion
import com.nabil.aireels.domain.repository.GeminiRepository
import com.nabil.aireels.domain.repository.VideoRepository
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class AutoReelResult(
    val script: ScriptSuggestion,
    val finalVideoPath: String
)

class GenerateAutoReelUseCase @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val videoRepository: VideoRepository,
    private val captionRenderer: CaptionRenderer,
    private val pexelsRepository: PexelsRepository
) {
    private val videoWidth = 1080
    private val videoHeight = 1920

    suspend operator fun invoke(
        topic: String,
        tone: String,
        durationSeconds: Int,
        imagePaths: List<String>,
        useAiPhotos: Boolean,
        captionsEnabled: Boolean,
        audioPath: String?,
        workingDir: File,
        onProgress: (String) -> Unit
    ): AppResult<AutoReelResult> {
        if (!useAiPhotos && imagePaths.isEmpty()) {
            return AppResult.Error("الرجاء إرفاق صورة واحدة على الأقل، أو تفعيل اختيار الصور تلقائياً")
        }

        onProgress("جاري توليد النص بالذكاء الاصطناعي...")
        val scriptResult = geminiRepository.generateReelScript(topic, tone, durationSeconds)
        val script = when (scriptResult) {
            is AppResult.Success -> scriptResult.data
            is AppResult.Error -> return AppResult.Error(scriptResult.message)
            AppResult.Loading -> return AppResult.Error("حالة غير متوقعة")
        }

        var finalImagePaths = imagePaths

        if (useAiPhotos) {
            if (script.imageQueries.isEmpty()) {
                return AppResult.Error("لم يقترح الذكاء الاصطناعي كلمات بحث للصور، حاول مرة أخرى")
            }
            onProgress("جاري جلب صور مناسبة تلقائياً...")
            val downloadedPaths = mutableListOf<String>()
            script.imageQueries.forEachIndexed { index, query ->
                val destFile = File(workingDir, "ai_photo_${index}_${UUID.randomUUID()}.jpg")
                when (val result = pexelsRepository.searchAndDownloadPhoto(query, destFile)) {
                    is AppResult.Success -> downloadedPaths.add(result.data)
                    is AppResult.Error -> return AppResult.Error(result.message)
                }
            }
            finalImagePaths = downloadedPaths
        }

        if (finalImagePaths.isEmpty()) {
            return AppResult.Error("لم يتم توفير أي صور صالحة لإنشاء الريلز")
        }

        onProgress("جاري إنشاء المقاطع المتحركة من الصور...")
        val perImageDuration = durationSeconds.toDouble() / finalImagePaths.size
        val segmentPaths = mutableListOf<Pair<String, Double>>()

        finalImagePaths.forEachIndexed { index, imagePath ->
            val segmentFile = File(workingDir, "segment_${index}_${UUID.randomUUID()}.mp4")
            when (val result = videoRepository.createKenBurnsSegment(
                imagePath = imagePath,
                durationSeconds = perImageDuration,
                outputPath = segmentFile.absolutePath,
                width = videoWidth,
                height = videoHeight
            )) {
                is AppResult.Success -> segmentPaths.add(result.data to perImageDuration)
                is AppResult.Error -> return AppResult.Error(result.message)
                AppResult.Loading -> Unit
            }
        }

        onProgress("جاري دمج المقاطع بانتقالات سلسة...")
        val transitionSeconds = if (segmentPaths.size > 1) {
            minOf(0.6, perImageDuration * 0.3)
        } else {
            0.0
        }

        val slideshowFile = File(workingDir, "slideshow_${UUID.randomUUID()}.mp4")
        val mergedResult = videoRepository.concatWithCrossfade(
            segments = segmentPaths,
            transitionSeconds = transitionSeconds,
            outputPath = slideshowFile.absolutePath
        )
        val slideshowPath = when (mergedResult) {
            is AppResult.Success -> mergedResult.data
            is AppResult.Error -> return AppResult.Error(mergedResult.message)
            AppResult.Loading -> return AppResult.Error("حالة غير متوقعة")
        }

        var videoAfterCaptions = slideshowPath

        if (captionsEnabled) {
            onProgress("جاري رسم الترجمة على الفيديو...")
            val captionOverlays = script.captionCues.mapIndexed { index, cue ->
                val pngFile = File(workingDir, "caption_${index}_${UUID.randomUUID()}.png")
                captionRenderer.renderCaptionPng(
                    text = cue.text,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    outputFile = pngFile
                )
                CaptionOverlay(
                    imagePath = pngFile.absolutePath,
                    startSeconds = cue.startSeconds,
                    endSeconds = cue.endSeconds
                )
            }

            val captionedFile = File(workingDir, "captioned_${UUID.randomUUID()}.mp4")
            when (val captionResult = videoRepository.overlayCaptionImages(
                videoPath = slideshowPath,
                captionOverlays = captionOverlays,
                outputPath = captionedFile.absolutePath
            )) {
                is AppResult.Success -> videoAfterCaptions = captionResult.data
                is AppResult.Error -> return AppResult.Error(captionResult.message)
                AppResult.Loading -> Unit
            }
        } else {
            val plainFile = File(workingDir, "plain_${UUID.randomUUID()}.mp4")
            when (val plainResult = videoRepository.overlayCaptionImages(
                videoPath = slideshowPath,
                captionOverlays = emptyList(),
                outputPath = plainFile.absolutePath
            )) {
                is AppResult.Success -> videoAfterCaptions = plainResult.data
                is AppResult.Error -> return AppResult.Error(plainResult.message)
                AppResult.Loading -> Unit
            }
        }

        var finalPath = videoAfterCaptions

        if (!audioPath.isNullOrBlank()) {
            onProgress("جاري إضافة الصوت...")
            val finalWithAudioFile = File(workingDir, "final_with_audio_${UUID.randomUUID()}.mp4")
            when (val audioResult = videoRepository.addAudioTrack(
                videoPath = videoAfterCaptions,
                audioPath = audioPath,
                outputPath = finalWithAudioFile.absolutePath
            )) {
                is AppResult.Success -> finalPath = audioResult.data
                is AppResult.Error -> return AppResult.Error(audioResult.message)
                AppResult.Loading -> Unit
            }
        }

        onProgress("اكتمل الريلز بنجاح!")
        return AppResult.Success(AutoReelResult(script = script, finalVideoPath = finalPath))
    }
}
