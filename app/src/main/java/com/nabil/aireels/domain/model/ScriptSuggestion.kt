package com.nabil.aireels.domain.model

data class ScriptSuggestion(
    val hook: String,
    val fullScript: String,
    val captions: List<String>,
    val captionCues: List<CaptionCue> = emptyList(),
    val imageQueries: List<String> = emptyList(),
    val colorGrade: String = "neutral",
    val hashtags: List<String>
)
