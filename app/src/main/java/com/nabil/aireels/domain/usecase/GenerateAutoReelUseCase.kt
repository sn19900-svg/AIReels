package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.render.CaptionRenderer
import com.nabil.aireels.domain.model.CaptionOverlay
import com.nabil.aireels.domain.model.Clip
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
    private val captionRenderer: CaptionRenderer
) {
    private val videoWidth = 1080
    private val videoHeight = 1920

    suspend operator fun invoke(
        topic: String,
        tone: String,
        durationSeconds: Int,
        imagePaths: List<String>,
        workingDir: File,
        onProgress: (String) -> Unit
    ): AppResult<AutoReelResult> {
        if (imagePaths.isEmpty()) {
            return AppResult.Error("الرجاء إرفاق صورة واحدة على الأقل")
        }

        onProgress("جاري توليد النص بالذكاء الاصطناعي...")
        val scriptResult = geminiRepository.generateReelScript(topic, tone, durationSeconds)
        val script = when (scriptResult) {
            is AppResult.Success -> scriptResult.data
            is AppResult.Error -> return AppResult.Error(scriptResult.message)
            AppResult.Loading -> return AppResult.Error("حالة غير متوقعة")
        }

        onProgress("جاري إنشاء المقاطع المتحركة من الصور...")
        val perImageDuration = durationSeconds.toDouble() / imagePaths.size
        val segmentPaths = mutableListOf<String>()

        imagePaths.forEachIndexed { index, imagePath ->
            val segmentFile = File(workingDir, "segment_${index}_${UUID.randomUUID()}.mp4")
            when (val result = videoRepository.createKenBurnsSegment(
                imagePath = imagePath,
                durationSeconds = perImageDuration,
                outputPath = segmentFile.absolutePath,
                width = videoWidth,
                height = videoHeight
            )) {
                is AppResult.Success -> segmentPaths.add(result.data)
                is AppResult.Error -> return AppResult.Error(result.message)
                AppResult.Loading -> Unit
            }
        }

        onProgress("جاري دمج المقاطع...")
        val clipsAsModel = segmentPaths.mapIndexed { index, path ->
            Clip(
                id = index.toString(),
                filePath = path,
                durationMs = (perImageDuration * 1000).toLong()
            )
        }
        val slideshowFile = File(workingDir, "slideshow_${UUID.randomUUID()}.mp4")
        val mergedResult = videoRepository.mergeClips(clipsAsModel, slideshowFile.absolutePath)
        val slideshowPath = when (mergedResult) {
            is AppResult.Success -> mergedResult.data
            is AppResult.Error -> return AppResult.Error(mergedResult.message)
            AppResult.Loading -> return AppResult.Error("حالة غير متوقعة")
        }

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

        val finalFile = File(workingDir, "final_reel_${UUID.randomUUID()}.mp4")
        val finalResult = videoRepository.overlayCaptionImages(
            videoPath = slideshowPath,
            captionOverlays = captionOverlays,
            outputPath = finalFile.absolutePath
        )

        return when (finalResult) {
            is AppResult.Success -> {
                onProgress("اكتمل الريلز بنجاح!")
                AppResult.Success(AutoReelResult(script = script, finalVideoPath = finalResult.data))
            }
            is AppResult.Error -> AppResult.Error(finalResult.message)
            AppResult.Loading -> AppResult.Error("حالة غير متوقعة")
        }
    }
}
