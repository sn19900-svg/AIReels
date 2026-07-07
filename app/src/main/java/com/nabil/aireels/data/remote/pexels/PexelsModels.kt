package com.nabil.aireels.data.remote.pexels

data class PexelsSearchResponse(
    val photos: List<PexelsPhoto>
)

data class PexelsPhoto(
    val src: PexelsPhotoSrc
)

data class PexelsPhotoSrc(
    val large2x: String,
    val large: String,
    val original: String
)
