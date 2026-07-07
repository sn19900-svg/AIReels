package com.nabil.aireels.feature.autoreel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.state.ProjectStateHolder
import com.nabil.aireels.domain.usecase.GenerateAutoReelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

data class AutoReelUiState(
    val topic: String = "",
    val tone: String = "حماسي",
    val durationSeconds: Int = 20,
    val selectedImagePaths: List<String> = emptyList(),
    val captionsEnabled: Boolean = true,
    val audioEnabled: Boolean = false,
    val selectedAudioPath: String? = null,
    val isProcessing: Boolean = false,
    val progressMessage: String = "",
    val resultVideoPath: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AutoReelViewModel @Inject constructor(
    private val generateAutoReelUseCase: GenerateAutoReelUseCase,
    private val projectStateHolder: ProjectStateHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutoReelUiState())
    val uiState: StateFlow<AutoReelUiState> = _uiState

    init {
        projectStateHolder.consumePendingAutoReelInput()?.let { (topic, tone) ->
            _uiState.value = _uiState.value.copy(topic = topic, tone = tone)
        }
    }

    fun onTopicChanged(value: String) {
        _uiState.value = _uiState.value.copy(topic = value)
    }

    fun onToneChanged(value: String) {
        _uiState.value = _uiState.value.copy(tone = value)
    }

    fun onCaptionsEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(captionsEnabled = enabled)
    }

    fun onAudioEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            audioEnabled = enabled,
            selectedAudioPath = if (!enabled) null else _uiState.value.selectedAudioPath
        )
    }

    fun onImagesSelected(context: Context, uris: List<Uri>) {
        val cacheDir = File(context.cacheDir, "autoreel_images").apply { mkdirs() }
        val copiedPaths = uris.mapNotNull { uri ->
            try {
                val destFile = File(cacheDir, "img_${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.absolutePath
            } catch (e: Exception) {
                null
            }
        }
        _uiState.value = _uiState.value.copy(selectedImagePaths = copiedPaths)
    }

    fun onAudioSelected(context: Context, uri: Uri) {
        try {
            val cacheDir = File(context.cacheDir, "autoreel_audio").apply { mkdirs() }
            val destFile = File(cacheDir, "audio_${UUID.randomUUID()}.mp3")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            _uiState.value = _uiState.value.copy(selectedAudioPath = destFile.absolutePath)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "فشل اختيار الملف الصوتي: ${e.message}")
        }
    }

    fun generateAutoReel(workingDir: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                errorMessage = null,
                resultVideoPath = null
            )

            val audioPathToUse = if (_uiState.value.audioEnabled) _uiState.value.selectedAudioPath else null

            val result = generateAutoReelUseCase(
                topic = _uiState.value.topic,
                tone = _uiState.value.tone,
                durationSeconds = _uiState.value.durationSeconds,
                imagePaths = _uiState.value.selectedImagePaths,
                captionsEnabled = _uiState.value.captionsEnabled,
                audioPath = audioPathToUse,
                workingDir = workingDir,
                onProgress = { message ->
                    _uiState.value = _uiState.value.copy(progressMessage = message)
                }
            )

            when (result) {
                is AppResult.Success -> {
                    projectStateHolder.updateProject { project ->
                        project.copy(
                            script = result.data.script,
                            mergedVideoPath = result.data.finalVideoPath
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        resultVideoPath = result.data.finalVideoPath
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = result.message
                    )
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
