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

    fun updateProject(transform: (ReelProject) -> ReelProject) {
        _currentProject.value = transform(_currentProject.value)
    }

    fun resetProject() {
        _currentProject.value = ReelProject(id = UUID.randomUUID().toString(), title = "مشروع بدون عنوان")
    }
}
