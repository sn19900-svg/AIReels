package com.nabil.aireels.data.repository

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.nabil.aireels.core.util.AppResult
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

        val listFile = File(outputPath).parentFile?.resolve("concat_list.txt")
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
}
