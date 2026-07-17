package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.render.CaptionRenderer
import com.nabil.aireels.data.repository.PexelsRepository
import com.nabil.aireels.domain.model.CaptionOverlay
import com.nabil.aireels.domain.model.KenBurnsMotionStyle
import com.nabil.aireels.domain.model.MediaSourceMode
import com.nabil.aireels.domain.model.ScriptSuggestion
import com.nabil.aireels.domain.repository.GeminiRepository
import com.nabil.aireels.domain.repository.VideoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
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

    private val motionStyleCycle = listOf(
        KenBurnsMotionStyle.ZOOM_IN,
        KenBurnsMotionStyle.PAN_LEFT_TO_RIGHT,
        KenBurnsMotionStyle.ZOOM_OUT,
        KenBurnsMotionStyle.PAN_RIGHT_TO_LEFT
    )

    private val transitionCycle = listOf(
        "fade",
        "smoothleft",
        "circleopen",
        "dissolve",
        "smoothright",
        "zoomin"
    )

    suspend operator fun invoke(
        topic: String,
        tone: String,
        durationSeconds: Int,
        imagePaths: List<String>,
        mediaSourceMode: MediaSourceMode,
        captionsEnabled: Boolean,
        audioPath: String?,
        workingDir: File,
        onProgress: (String) -> Unit
    ): AppResult<AutoReelResult> = coroutineScope {
        if (mediaSourceMode == MediaSourceMode.USER_PHOTOS && imagePaths.isEmpty()) {
            return@coroutineScope AppResult.Error("الرجاء إرفاق صورة واحدة على الأقل، أو اختيار وضع تلقائي")
        }

        onProgress("جاري توليد النص بالذكاء الاصطناعي...")
        val scriptResult = geminiRepository.generateReelScript(topic, tone, durationSeconds)
        val script = when (scriptResult) {
            is AppResult.Success -> scriptResult.data
            is AppResult.Error -> return@coroutineScope AppResult.Error(scriptResult.message)
            AppResult.Loading -> return@coroutineScope AppResult.Error("حالة غير متوقعة")
        }

        val itemCount = if (mediaSourceMode == MediaSourceMode.USER_PHOTOS) {
            imagePaths.size
        } else {
            script.imageQueries.size.coerceAtLeast(1)
        }
        val perItemDuration = durationSeconds.toDouble() / itemCount
        val segmentPaths = mutableListOf<Pair<String, Double>>()

        when (mediaSourceMode) {
            MediaSourceMode.HYBRID_HERO_PLUS_AI -> {
                if (script.imageQueries.isEmpty()) {
                    return@coroutineScope AppResult.Error("لم يقترح الذكاء الاصطناعي كلمات بحث للمشاهد التمهيدية")
                }
                onProgress("جاري جلب ${script.imageQueries.size} مشهد تمهيدي بالتوازي...")

                val heroWeight = 1.5
                val totalWeight = script.imageQueries.size + imagePaths.size * heroWeight
                val unitDuration = durationSeconds.toDouble() / totalWeight
                val brollDuration = unitDuration
                val heroDuration = unitDuration * heroWeight

                val completedCounterHybrid = AtomicInteger(0)
                val totalToFetchHybrid = script.imageQueries.size
                val downloadJobs = script.imageQueries.mapIndexed { index, query ->
                    async {
                        val photoFile = File(workingDir, "broll_photo_${index}_${UUID.randomUUID()}.jpg")
                        val result = pexelsRepository.searchAndDownloadPhoto(query, photoFile)
                        onProgress("تم جلب ${completedCounterHybrid.incrementAndGet()} من $totalToFetchHybrid مشهد تمهيدي...")
                        result
                    }
                }
                val downloadResults = downloadJobs.map { it.await() }

                for ((index, downloadResult) in downloadResults.withIndex()) {
                    val downloadedPath = when (downloadResult) {
                        is AppResult.Success -> downloadResult.data
                        is AppResult.Error -> return@coroutineScope AppResult.Error(downloadResult.message)
                        AppResult.Loading -> continue
                    }
                    onProgress("جاري تحريك المشهد ${index + 1} من ${downloadResults.size}...")
                    val segmentFile = File(workingDir, "broll_segment_${index}_${UUID.randomUUID()}.mp4")
                    val motionStyle = motionStyleCycle[index % motionStyleCycle.size]
                    val kbResult = videoRepository.createKenBurnsSegment(
                        downloadedPath, brollDuration, segmentFile.absolutePath, videoWidth, videoHeight, motionStyle
                    )
                    when (kbResult) {
                        is AppResult.Success -> segmentPaths.add(kbResult.data to brollDuration)
                        is AppResult.Error -> return@coroutineScope AppResult.Error(kbResult.message)
                        AppResult.Loading -> Unit
                    }
                }

                onProgress("جاري تجهيز اللقطة البطولية...")
                for ((index, heroImagePath) in imagePaths.withIndex()) {
                    val heroSegmentFile = File(workingDir, "hero_segment_${index}_${UUID.randomUUID()}.mp4")
                    val heroResult = videoRepository.createHeroImageSegment(
                        heroImagePath, heroDuration, heroSegmentFile.absolutePath, videoWidth, videoHeight
                    )
                    when (heroResult) {
                        is AppResult.Success -> segmentPaths.add(heroResult.data to heroDuration)
                        is AppResult.Error -> return@coroutineScope AppResult.Error(heroResult.message)
                        AppResult.Loading -> Unit
                    }
                }
            }

            MediaSourceMode.USER_PHOTOS -> {
                for ((index, imagePath) in imagePaths.withIndex()) {
                    onProgress("جاري تحريك الصورة ${index + 1} من ${imagePaths.size}...")
                    val segmentFile = File(workingDir, "segment_${index}_${UUID.randomUUID()}.mp4")
                    val motionStyle = motionStyleCycle[index % motionStyleCycle.size]
                    val kbResult = videoRepository.createKenBurnsSegment(
                        imagePath, perItemDuration, segmentFile.absolutePath, videoWidth, videoHeight, motionStyle
                    )
                    when (kbResult) {
                        is AppResult.Success -> segmentPaths.add(kbResult.data to perItemDuration)
                        is AppResult.Error -> return@coroutineScope AppResult.Error(kbResult.message)
                        AppResult.Loading -> Unit
                    }
                }
            }

            MediaSourceMode.AI_STOCK_PHOTOS -> {
                if (script.imageQueries.isEmpty()) {
                    return@coroutineScope AppResult.Error("لم يقترح الذكاء الاصطناعي كلمات بحث للصور")
                }
                val completedCounterPhotos = AtomicInteger(0)
                val totalToFetchPhotos = script.imageQueries.size
                onProgress("جاري جلب $totalToFetchPhotos صورة بالتوازي...")
                val downloadJobs = script.imageQueries.mapIndexed { index, query ->
                    async {
                        val photoFile = File(workingDir, "ai_photo_${index}_${UUID.randomUUID()}.jpg")
                        val result = pexelsRepository.searchAndDownloadPhoto(query, photoFile)
                        onProgress("تم جلب ${completedCounterPhotos.incrementAndGet()} من $totalToFetchPhotos صورة...")
                        result
                    }
                }
                val downloadResults = downloadJobs.map { it.await() }

                for ((index, downloadResult) in downloadResults.withIndex()) {
                    val downloadedPath = when (downloadResult) {
                        is AppResult.Success -> downloadResult.data
                        is AppResult.Error -> return@coroutineScope AppResult.Error(downloadResult.message)
                        AppResult.Loading -> continue
                    }
                    onProgress("جاري تحريك المشهد ${index + 1} من ${downloadResults.size}...")
                    val segmentFile = File(workingDir, "segment_${index}_${UUID.randomUUID()}.mp4")
                    val motionStyle = motionStyleCycle[index % motionStyleCycle.size]
                    val kbResult = videoRepository.createKenBurnsSegment(
                        downloadedPath, perItemDuration, segmentFile.absolutePath, videoWidth, videoHeight, motionStyle
                    )
                    when (kbResult) {
                        is AppResult.Success -> segmentPaths.add(kbResult.data to perItemDuration)
                        is AppResult.Error -> return@coroutineScope AppResult.Error(kbResult.message)
                        AppResult.Loading -> Unit
                    }
                }
            }

            MediaSourceMode.AI_STOCK_VIDEO -> {
                if (script.imageQueries.isEmpty()) {
                    return@coroutineScope AppResult.Error("لم يقترح الذكاء الاصطناعي كلمات بحث للمشاهد")
                }
                val completedCounterVideos = AtomicInteger(0)
                val totalToFetchVideos = script.imageQueries.size
                onProgress("جاري جلب $totalToFetchVideos مقطع فيديو بالتوازي...")
                val downloadJobs = script.imageQueries.mapIndexed { index, query ->
                    async {
                        val videoFile = File(workingDir, "ai_video_${index}_${UUID.randomUUID()}.mp4")
                        val result = pexelsRepository.searchAndDownloadVideo(query, videoFile)
                        onProgress("تم جلب ${completedCounterVideos.incrementAndGet()} من $totalToFetchVideos مقطع فيديو...")
                        result
                    }
                }
                val downloadResults = downloadJobs.map { it.await() }

                for ((index, downloadResult) in downloadResults.withIndex()) {
                    val downloadedPath = when (downloadResult) {
                        is AppResult.Success -> downloadResult.data
                        is AppResult.Error -> return@coroutineScope AppResult.Error(downloadResult.message)
                        AppResult.Loading -> continue
                    }
                    onProgress("جاري تجهيز المشهد ${index + 1} من ${downloadResults.size}...")
                    val segmentFile = File(workingDir, "segment_${index}_${UUID.randomUUID()}.mp4")
                    val prepResult = videoRepository.prepareStockVideoSegment(
                        downloadedPath, perItemDuration, segmentFile.absolutePath, videoWidth, videoHeight
                    )
                    when (prepResult) {
                        is AppResult.Success -> segmentPaths.add(prepResult.data to perItemDuration)
                        is AppResult.Error -> return@coroutineScope AppResult.Error(prepResult.message)
                        AppResult.Loading -> Unit
                    }
                }
            }
        }

        onProgress("جاري دمج المقاطع بانتقالات سينمائية متنوعة...")
        val transitionSeconds = if (segmentPaths.size > 1) minOf(0.6, perItemDuration * 0.3) else 0.0
        val transitionNamesForThisReel = (0 until (segmentPaths.size - 1).coerceAtLeast(0)).map { i ->
            transitionCycle[i % transitionCycle.size]
        }

        val slideshowFile = File(workingDir, "slideshow_${UUID.randomUUID()}.mp4")
        val mergedResult = videoRepository.concatWithCrossfade(
            segments = segmentPaths,
            transitionSeconds = transitionSeconds,
            transitionNames = transitionNamesForThisReel,
            outputPath = slideshowFile.absolutePath
        )
        val slideshowPath = when (mergedResult) {
            is AppResult.Success -> mergedResult.data
            is AppResult.Error -> return@coroutineScope AppResult.Error(mergedResult.message)
            AppResult.Loading -> return@coroutineScope AppResult.Error("حالة غير متوقعة")
        }

        var videoAfterCaptions = slideshowPath

        onProgress(
            if (captionsEnabled) "جاري تطبيق تدريج الألوان ورسم الترجمة..." else "جاري تطبيق تدريج الألوان..."
        )
        val captionOverlays = if (captionsEnabled) {
            script.captionCues.mapIndexed { index, cue ->
                val pngFile = File(workingDir, "caption_${index}_${UUID.randomUUID()}.png")
                captionRenderer.renderCaptionPng(cue.text, videoWidth, videoHeight, pngFile)
                CaptionOverlay(pngFile.absolutePath, cue.startSeconds, cue.endSeconds)
            }
        } else {
            emptyList()
        }

        val processedFile = File(workingDir, "processed_${UUID.randomUUID()}.mp4")
        val processedResult = videoRepository.overlayCaptionImages(
            slideshowPath, captionOverlays, script.colorGrade, processedFile.absolutePath
        )
        when (processedResult) {
            is AppResult.Success -> videoAfterCaptions = processedResult.data
            is AppResult.Error -> return@coroutineScope AppResult.Error(processedResult.message)
            AppResult.Loading -> Unit
        }

        var finalPath = videoAfterCaptions

        if (!audioPath.isNullOrBlank()) {
            onProgress("جاري إضافة الصوت...")
            val finalWithAudioFile = File(workingDir, "final_with_audio_${UUID.randomUUID()}.mp4")
            val audioResult = videoRepository.addAudioTrack(
                videoAfterCaptions, audioPath, finalWithAudioFile.absolutePath
            )
            when (audioResult) {
                is AppResult.Success -> finalPath = audioResult.data
                is AppResult.Error -> return@coroutineScope AppResult.Error(audioResult.message)
                AppResult.Loading -> Unit
            }
        }

        onProgress("اكتمل الريلز بنجاح!")
        AppResult.Success(AutoReelResult(script = script, finalVideoPath = finalPath))
    }
}
