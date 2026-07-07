package com.nabil.aireels.data.repository

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.CaptionOverlay
import com.nabil.aireels.domain.model.Clip
import com.nabil.aireels.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FfmpegVideoRepositoryImpl @Inject constructor() : VideoRepository {

    override suspend fun trimClip(
        inputPath: String,
        startMs: Long,
        endMs: Long,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val startSeconds = startMs / 1000.0
        val durationSeconds = (endMs - startMs) / 1000.0
        val command = "-y -i \"$inputPath\" -ss $startSeconds -t $durationSeconds " +
            "-c:v mpeg4 -c:a aac \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل قص المقطع: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun mergeClips(
        clips: List<Clip>,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        if (clips.isEmpty()) {
            return@withContext AppResult.Error("لا توجد مقاطع للدمج")
        }

        val listFile = File(outputPath).parentFile?.resolve("concat_list_${System.nanoTime()}.txt")
            ?: return@withContext AppResult.Error("مسار الإخراج غير صالح")

        listFile.bufferedWriter().use { writer ->
            clips.forEach { clip ->
                writer.write("file '${clip.filePath}'")
                writer.newLine()
            }
        }

        val command = "-y -f concat -safe 0 -i \"${listFile.absolutePath}\" " +
            "-c:v mpeg4 -c:a aac \"$outputPath\""

        val session = FFmpegKit.execute(command)
        listFile.delete()

        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل دمج المقاطع: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun addAudioTrack(
        videoPath: String,
        audioPath: String,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val command = "-y -i \"$videoPath\" -i \"$audioPath\" " +
            "-c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل إضافة المسار الصوتي: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun extractAudio(
        videoPath: String,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val command = "-y -i \"$videoPath\" -vn -ac 1 -ar 16000 -f wav \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل استخراج الصوت: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun createKenBurnsSegment(
        imagePath: String,
        durationSeconds: Double,
        outputPath: String,
        width: Int,
        height: Int
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val fps = 30
        val totalFrames = (durationSeconds * fps).toInt().coerceAtLeast(fps)
        val command = "-y -loop 1 -i \"$imagePath\" -t $durationSeconds " +
            "-vf \"scale=${width * 2}:-1,zoompan=z='zoom+0.0015':d=$totalFrames:s=${width}x${height}:fps=$fps,format=yuv420p\" " +
            "-c:v mpeg4 -an \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل إنشاء مقطع الصورة المتحركة: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun overlayCaptionImages(
        videoPath: String,
        captionOverlays: List<CaptionOverlay>,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        if (captionOverlays.isEmpty()) {
            val copySession = FFmpegKit.execute("-y -i \"$videoPath\" -c copy \"$outputPath\"")
            return@withContext if (ReturnCode.isSuccess(copySession.returnCode)) {
                AppResult.Success(outputPath)
            } else {
                AppResult.Error("فشل نسخ الفيديو: ${copySession.allLogsAsString}")
            }
        }

        val inputsBuilder = StringBuilder("-y -i \"$videoPath\" ")
        captionOverlays.forEach { overlay ->
            inputsBuilder.append("-i \"${overlay.imagePath}\" ")
        }

        val filterBuilder = StringBuilder()
        var lastLabel = "0:v"
        captionOverlays.forEachIndexed { index, overlay ->
            val inputIndex = index + 1
            val outputLabel = "v${index + 1}"
            filterBuilder.append(
                "[$lastLabel][$inputIndex:v]overlay=0:0:enable='between(t,${overlay.startSeconds},${overlay.endSeconds})'[$outputLabel];"
            )
            lastLabel = outputLabel
        }

        val filterComplex = filterBuilder.toString().trimEnd(';')
        val command = "$inputsBuilder-filter_complex \"$filterComplex\" -map \"[$lastLabel]\" -map 0:a? " +
            "-c:v mpeg4 -c:a aac \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل دمج الترجمات على الفيديو: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }
}
