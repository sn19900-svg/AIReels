package com.nabil.aireels.data.state

import com.nabil.aireels.domain.model.ReelProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectStateHolder @Inject constructor() {

    private val _currentProject = MutableStateFlow(
        ReelProject(id = UUID.randomUUID().toString(), title = "مشروع بدون عنوان")
    )
    val currentProject: StateFlow<ReelProject> = _currentProject

    private val _pendingAutoReelTopic = MutableStateFlow<String?>(null)
    val pendingAutoReelTopic: StateFlow<String?> = _pendingAutoReelTopic

    private val _pendingAutoReelTone = MutableStateFlow<String?>(null)
    val pendingAutoReelTone: StateFlow<String?> = _pendingAutoReelTone

    fun updateProject(transform: (ReelProject) -> ReelProject) {
        _currentProject.value = transform(_currentProject.value)
    }

    fun resetProject() {
        _currentProject.value = ReelProject(id = UUID.randomUUID().toString(), title = "مشروع بدون عنوان")
    }

    fun setPendingAutoReelInput(topic: String, tone: String) {
        _pendingAutoReelTopic.value = topic
        _pendingAutoReelTone.value = tone
    }

    fun consumePendingAutoReelInput(): Pair<String, String>? {
        val topic = _pendingAutoReelTopic.value
        val tone = _pendingAutoReelTone.value
        return if (topic != null && tone != null) {
            _pendingAutoReelTopic.value = null
            _pendingAutoReelTone.value = null
            topic to tone
        } else {
            null
        }
    }
}
