package com.nabil.aireels.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabil.aireels.data.state.ProjectStateHolder
import com.nabil.aireels.domain.model.Clip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

data class CameraUiState(
    val isRecording: Boolean = false,
    val recordedClipsCount: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val projectStateHolder: ProjectStateHolder
) : ViewModel() {

    private val _localState = MutableStateFlow(CameraUiState())

    val uiState: StateFlow<CameraUiState> = combine(
        _localState,
        projectStateHolder.currentProject
    ) { local, project ->
        local.copy(recordedClipsCount = project.clips.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CameraUiState())

    fun onRecordingStarted() {
        _localState.value = _localState.value.copy(isRecording = true, errorMessage = null)
    }

    fun onRecordingFinished(filePath: String) {
        val durationMs = estimateDurationMs(filePath)
        val newClip = Clip(
            id = UUID.randomUUID().toString(),
            filePath = filePath,
            durationMs = durationMs,
            trimStartMs = 0L,
            trimEndMs = durationMs
        )
        projectStateHolder.updateProject { project ->
            project.copy(clips = project.clips + newClip)
        }
        _localState.value = _localState.value.copy(isRecording = false)
    }

    fun onRecordingError(message: String) {
        _localState.value = _localState.value.copy(isRecording = false, errorMessage = message)
    }

    private fun estimateDurationMs(filePath: String): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val duration = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
