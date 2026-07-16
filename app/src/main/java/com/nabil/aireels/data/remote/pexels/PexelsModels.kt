package com.nabil.aireels.data.remote.pexels

import com.google.gson.annotations.SerializedName

data class PexelsSearchResponse(
    val photos: List<PexelsPhoto>
)

data class PexelsPhoto(
    val width: Int,
    val height: Int,
    val src: PexelsPhotoSrc
)

data class PexelsPhotoSrc(
    val large2x: String,
    val large: String,
    val original: String
)

data class PexelsVideoSearchResponse(
    val videos: List<PexelsVideo>
)

data class PexelsVideo(
    val duration: Int,
    @SerializedName("video_files")
    val videoFiles: List<PexelsVideoFile>
)

data class PexelsVideoFile(
    val link: String,
    val quality: String?,
    val width: Int?,
    val height: Int?,
    @SerializedName("file_type")
    val fileType: String?
)
