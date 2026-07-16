package com.nabil.aireels.data.remote.pexels

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PexelsApiService {
    @GET("v1/search")
    suspend fun searchPhotos(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 6,
        @Query("orientation") orientation: String = "portrait"
    ): PexelsSearchResponse

    @GET("videos/search")
    suspend fun searchVideos(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 6,
        @Query("orientation") orientation: String = "portrait"
    ): PexelsVideoSearchResponse
}
