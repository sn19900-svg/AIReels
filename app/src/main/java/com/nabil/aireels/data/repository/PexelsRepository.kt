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

                val request = Request.Builder().url(photoUrl).build()
                okHttpClient.newCall(request).execute().use { httpResponse ->
                    if (!httpResponse.isSuccessful) {
                        return@withContext AppResult.Error("فشل تحميل الصورة من Pexels")
                    }
                    val body = httpResponse.body
                        ?: return@withContext AppResult.Error("لا يوجد محتوى للصورة")
                    body.byteStream().use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                AppResult.Success(destFile.absolutePath)
            } catch (e: Exception) {
                AppResult.Error("فشل تحميل صورة من Pexels: ${e.message}", e)
            }
        }
}
