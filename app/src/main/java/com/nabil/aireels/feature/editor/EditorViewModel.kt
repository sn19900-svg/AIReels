package com.nabil.aireels.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.state.ProjectStateHolder
import com.nabil.aireels.domain.model.Clip
import com.nabil.aireels.domain.usecase.MergeClipsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class EditorUiState(
    val clips: List<Clip> = emptyList(),
    val isMerging: Boolean = false,
    val mergedVideoPath: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val projectStateHolder: ProjectStateHolder,
    private val mergeClipsUseCase: MergeClipsUseCase
) : ViewModel() {

    private val _localState = MutableStateFlow(EditorUiState())

    val uiState: StateFlow<EditorUiState> = combine(
        _localState,
        projectStateHolder.currentProject
    ) { local, project ->
        local.copy(clips = project.clips, mergedVideoPath = project.mergedVideoPath)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EditorUiState())

    fun removeClip(clipId: String) {
        projectStateHolder.updateProject { project ->
            project.copy(clips = project.clips.filterNot { it.id == clipId })
        }
    }

    fun mergeClips(outputDir: File) {
        viewModelScope.launch {
            _localState.value = _localState.value.copy(isMerging = true, errorMessage = null)
            val currentClips = projectStateHolder.currentProject.value.clips
            val outputFile = File(outputDir, "merged_${System.currentTimeMillis()}.mp4")

            when (val result = mergeClipsUseCase(currentClips, outputFile.absolutePath)) {
                is AppResult.Success -> {
                    projectStateHolder.updateProject { it.copy(mergedVideoPath = result.data) }
                    _localState.value = _localState.value.copy(isMerging = false)
                }
                is AppResult.Error -> {
                    _localState.value = _localState.value.copy(isMerging = false, errorMessage = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
