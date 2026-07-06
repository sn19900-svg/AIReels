package com.nabil.aireels.feature.captions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.state.ProjectStateHolder
import com.nabil.aireels.domain.repository.VideoRepository
import com.nabil.aireels.domain.usecase.TranscribeAudioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CaptionsUiState(
    val isProcessing: Boolean = false,
    val transcriptText: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class CaptionsViewModel @Inject constructor(
    private val projectStateHolder: ProjectStateHolder,
    private val videoRepository: VideoRepository,
    private val transcribeAudioUseCase: TranscribeAudioUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptionsUiState())
    val uiState: StateFlow<CaptionsUiState> = _uiState

    fun generateCaptions(workingDir: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)

            val mergedVideoPath = projectStateHolder.currentProject.value.mergedVideoPath
            if (mergedVideoPath == null) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "الرجاء دمج المقاطع أولاً في المحرر"
                )
                return@launch
            }

            val audioFile = File(workingDir, "extracted_audio.wav")
            when (val audioResult = videoRepository.extractAudio(mergedVideoPath, audioFile.absolutePath)) {
                is AppResult.Success -> {
                    when (val transcriptResult = transcribeAudioUseCase(audioResult.data)) {
                        is AppResult.Success -> {
                            val fullText = transcriptResult.data.joinToString("\n") { it.text }
                            projectStateHolder.updateProject { project ->
                                project.copy(transcriptSegments = transcriptResult.data)
                            }
                            _uiState.value = _uiState.value.copy(
                                isProcessing = false,
                                transcriptText = fullText
                            )
                        }
                        is AppResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isProcessing = false,
                                errorMessage = transcriptResult.message
                            )
                        }
                        AppResult.Loading -> Unit
                    }
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(isProcessing = false, errorMessage = audioResult.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
