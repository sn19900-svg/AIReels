package com.nabil.aireels.domain.model

data class TranscriptSegment(
    val text: String,
    val startMs: Long,
    val endMs: Long
)
