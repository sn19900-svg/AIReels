package com.nabil.aireels.domain.model

data class ScriptSuggestion(
    val hook: String,
    val fullScript: String,
    val captions: List<String>,
    val hashtags: List<String>
)
