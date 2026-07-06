package com.nabil.aireels.domain.model

data class ReelProject(
    val id: String,
    val title: String,
    val clips: List<Clip> = emptyList(),
    val backgroundMusicPath: String? = null,
    val mergedVideoPath: String? = null,
    val script: ScriptSuggestion? = null,
    val transcriptSegments: List<TranscriptSegment> = emptyList()
)
