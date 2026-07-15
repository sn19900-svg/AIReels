package com.nabil.aireels.data.repository

import com.nabil.aireels.BuildConfig
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.remote.pexels.PexelsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PexelsRepository @Inject constructor(
    private val pexelsApiService: PexelsApiService,
    private val okHttpClient: OkHttpClient
) {
    suspend fun searchAndDownloadPhoto(query: String, destFile: File): AppResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.PEXELS_API_KEY
                if (apiKey.isBlank()) {
                    return@withContext AppResult.Error(
                        "مفتاح Pexels API غير موجود. أضفه في local.properties أو GitHub Secrets"
                    )
                }

                val response = pexelsApiService.searchPhotos(apiKey = apiKey, query = query)
                val photoUrl = response.photos.firstOrNull()?.src?.large2x
                    ?: return@withContext AppResult.Error("لم يتم العثور على صورة مناسبة لـ: $query")

                downloadToFile(photoUrl, destFile)
            } catch (e: Exception) {
                AppResult.Error("فشل تحميل صورة من Pexels: ${e.message}", e)
            }
        }

    suspend fun searchAndDownloadVideo(query: String, destFile: File): AppResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.PEXELS_API_KEY
                if (apiKey.isBlank()) {
                    return@withContext AppResult.Error(
                        "مفتاح Pexels API غير موجود. أضفه في local.properties أو GitHub Secrets"
                    )
                }

                val response = pexelsApiService.searchVideos(apiKey = apiKey, query = query)
                val video = response.videos.firstOrNull()
                    ?: return@withContext AppResult.Error("لم يتم العثور على مقطع فيديو مناسب لـ: $query")

                val bestFile = video.videoFiles
                    .filter { file -> (file.height ?: 0) >= (file.width ?: 0) && (file.height ?: 0) in 720..1920 }
                    .maxByOrNull { file -> file.height ?: 0 }
                    ?: video.videoFiles.maxByOrNull { file -> (file.width ?: 0) * (file.height ?: 0) }
                    ?: return@withContext AppResult.Error("لا توجد ملفات فيديو صالحة لـ: $query")

                downloadToFile(bestFile.link, destFile)
            } catch (e: Exception) {
                AppResult.Error("فشل تحميل فيديو من Pexels: ${e.message}", e)
            }
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
