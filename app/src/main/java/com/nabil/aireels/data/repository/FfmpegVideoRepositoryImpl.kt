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
            "-c:v mpeg4 -q:v 3 -c:a aac \"$outputPath\""

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
            "-c:v mpeg4 -q:v 3 -c:a aac \"$outputPath\""

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
            "-c:v copy -c:a aac -b:a 128k -map 0:v:0 -map 1:a:0 -shortest -movflags +faststart \"$outputPath\""

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
        val bigWidth = width * 2
        val bigHeight = height * 2

        val filterComplex =
            "[0:v]scale=$bigWidth:$bigHeight:force_original_aspect_ratio=increase," +
                "crop=$bigWidth:$bigHeight,gblur=sigma=30,eq=brightness=-0.08[bg];" +
                "[1:v]scale=$bigWidth:$bigHeight:force_original_aspect_ratio=decrease[fg];" +
                "[bg][fg]overlay=(W-w)/2:(H-h)/2," +
                "zoompan=z='min(zoom+0.0006,1.12)':d=$totalFrames:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=${width}x${height}:fps=$fps," +
                "format=yuv420p[vout]"

        val command = "-y -loop 1 -t $durationSeconds -i \"$imagePath\" " +
            "-loop 1 -t $durationSeconds -i \"$imagePath\" " +
            "-filter_complex \"$filterComplex\" -map \"[vout]\" -c:v mpeg4 -q:v 3 -an \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل إنشاء مقطع الصورة المتحركة: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun prepareStockVideoSegment(
        videoPath: String,
        durationSeconds: Double,
        outputPath: String,
        width: Int,
        height: Int
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val vf = "scale=$width:$height:force_original_aspect_ratio=increase," +
            "crop=$width:$height,fps=30,format=yuv420p"

        val command = "-y -stream_loop -1 -i \"$videoPath\" -t $durationSeconds " +
            "-vf \"$vf\" -c:v mpeg4 -q:v 3 -an \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل تجهيز مقطع الفيديو الجاهز: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun concatWithCrossfade(
        segments: List<Pair<String, Double>>,
        transitionSeconds: Double,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        if (segments.isEmpty()) {
            return@withContext AppResult.Error("لا توجد مقاطع للدمج")
        }

        if (segments.size == 1) {
            val singleCommand = "-y -i \"${segments[0].first}\" " +
                "-c:v mpeg4 -q:v 3 -pix_fmt yuv420p -an \"$outputPath\""
            val session = FFmpegKit.execute(singleCommand)
            return@withContext if (ReturnCode.isSuccess(session.returnCode)) {
                AppResult.Success(outputPath)
            } else {
                AppResult.Error("فشل تجهيز المقطع: ${session.allLogsAsString}")
            }
        }

        val inputsBuilder = StringBuilder("-y ")
        segments.forEach { (path, _) ->
            inputsBuilder.append("-i \"$path\" ")
        }

        val filterBuilder = StringBuilder()
        var cumulativeDuration = segments[0].second
        var lastLabel = "0:v"

        for (i in 1 until segments.size) {
            val nextLabel = if (i == segments.size - 1) "vout" else "x$i"
            val offset = (cumulativeDuration - transitionSeconds).coerceAtLeast(0.0)
            filterBuilder.append(
                "[$lastLabel][$i:v]xfade=transition=fade:duration=$transitionSeconds:offset=$offset[$nextLabel];"
            )
            cumulativeDuration = cumulativeDuration + segments[i].second - transitionSeconds
            lastLabel = nextLabel
        }

        val filterComplex = filterBuilder.toString().trimEnd(';')
        val command = "$inputsBuilder-filter_complex \"$filterComplex\" -map \"[$lastLabel]\" " +
            "-c:v mpeg4 -q:v 3 -pix_fmt yuv420p \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل دمج المقاطع بانتقالات سلسة: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun overlayCaptionImages(
        videoPath: String,
        captionOverlays: List<CaptionOverlay>,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        if (captionOverlays.isEmpty()) {
            val copySession = FFmpegKit.execute(
                "-y -i \"$videoPath\" -c:v mpeg4 -q:v 3 -pix_fmt yuv420p " +
                    "-c:a aac -b:a 128k -movflags +faststart \"$outputPath\""
            )
            return@withContext if (ReturnCode.isSuccess(copySession.returnCode)) {
                AppResult.Success(outputPath)
            } else {
                AppResult.Error("فشل معالجة الفيديو: ${copySession.allLogsAsString}")
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
            "-c:v mpeg4 -q:v 3 -pix_fmt yuv420p -c:a aac -b:a 128k -movflags +faststart \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل دمج الترجمات على الفيديو: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }
}
