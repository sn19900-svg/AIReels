package com.nabil.aireels.domain.model

data class Clip(
    val id: String,
    val filePath: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs
)
