package com.nabil.aireels.data.repository

import com.nabil.aireels.BuildConfig
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.remote.pexels.PexelsApiService
import com.nabil.aireels.data.remote.pexels.PexelsVideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class PexelsRepository @Inject constructor(
    private val pexelsApiService: PexelsApiService,
    private val okHttpClient: OkHttpClient
) {
    suspend fun searchAndDownloadPhoto(query: String, destFile: File): AppResult<String> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(25_000L) {
                    val apiKey = BuildConfig.PEXELS_API_KEY
                    if (apiKey.isBlank()) {
                        return@withTimeout AppResult.Error(
                            "مفتاح Pexels API غير موجود. أضفه في local.properties أو GitHub Secrets"
                        )
                    }

                    val response = pexelsApiService.searchPhotos(apiKey = apiKey, query = query)
                    if (response.photos.isEmpty()) {
                        return@withTimeout AppResult.Error("لم يتم العثور على صورة مناسبة لـ: $query")
                    }

                    val portraitPhotos = response.photos.filter { it.height > it.width }
                    val candidates = portraitPhotos.ifEmpty { response.photos }
                    val topCandidates = candidates.take(3)
                    val chosenPhoto = topCandidates[Random.nextInt(topCandidates.size)]

                    downloadToFile(chosenPhoto.src.large, destFile)
                }
            } catch (e: TimeoutCancellationException) {
                AppResult.Error("انتهت مهلة جلب الصورة لـ: $query (تحقق من الاتصال بالإنترنت)")
            } catch (e: Exception) {
                AppResult.Error("فشل تحميل صورة من Pexels: ${e.message}", e)
            }
        }

    suspend fun searchAndDownloadVideo(query: String, destFile: File): AppResult<String> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(40_000L) {
                    val apiKey = BuildConfig.PEXELS_API_KEY
                    if (apiKey.isBlank()) {
                        return@withTimeout AppResult.Error(
                            "مفتاح Pexels API غير موجود. أضفه في local.properties أو GitHub Secrets"
                        )
                    }

                    val response = pexelsApiService.searchVideos(apiKey = apiKey, query = query)
                    if (response.videos.isEmpty()) {
                        return@withTimeout AppResult.Error("لم يتم العثور على مقطع فيديو مناسب لـ: $query")
                    }

                    val topVideos = response.videos.take(3)
                    val chosenVideo = topVideos[Random.nextInt(topVideos.size)]

                    val bestFile = selectBestVideoFile(chosenVideo.videoFiles)
                        ?: return@withTimeout AppResult.Error("لا توجد ملفات فيديو صالحة لـ: $query")

                    downloadToFile(bestFile.link, destFile)
                }
            } catch (e: TimeoutCancellationException) {
                AppResult.Error("انتهت مهلة جلب الفيديو لـ: $query (تحقق من الاتصال بالإنترنت)")
            } catch (e: Exception) {
                AppResult.Error("فشل تحميل فيديو من Pexels: ${e.message}", e)
            }
        }

    private fun selectBestVideoFile(files: List<PexelsVideoFile>): PexelsVideoFile? {
        val portraitMp4Files = files.filter { file ->
            val width = file.width ?: 0
            val height = file.height ?: 0
            file.fileType == "video/mp4" && height > width && height in 480..900
        }

        return portraitMp4Files.maxByOrNull { it.height ?: 0 }
            ?: files.filter { file ->
                val width = file.width ?: 0
                val height = file.height ?: 0
                file.fileType == "video/mp4" && height > width
            }.minByOrNull { it.height ?: Int.MAX_VALUE }
            ?: files.filter { it.fileType == "video/mp4" }
                .minByOrNull { (it.width ?: 0) * (it.height ?: 0) }
    }

    private fun downloadToFile(url: String, destFile: File): AppResult<String> {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { httpResponse ->
            if (!httpResponse.isSuccessful) {
                return AppResult.Error("فشل تحميل الملف من Pexels")
            }
            val body = httpResponse.body ?: return AppResult.Error("لا يوجد محتوى للملف")
            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return AppResult.Success(destFile.absolutePath)
    }
}
