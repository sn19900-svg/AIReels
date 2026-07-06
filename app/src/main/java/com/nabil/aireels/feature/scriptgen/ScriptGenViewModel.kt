package com.nabil.aireels.feature.scriptgen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.ScriptSuggestion
import com.nabil.aireels.domain.usecase.GenerateReelScriptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScriptGenUiState(
    val topic: String = "",
    val tone: String = "حماسي",
    val durationSeconds: Int = 30,
    val isLoading: Boolean = false,
    val suggestion: ScriptSuggestion? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ScriptGenViewModel @Inject constructor(
    private val generateReelScriptUseCase: GenerateReelScriptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptGenUiState())
    val uiState: StateFlow<ScriptGenUiState> = _uiState

    fun onTopicChanged(value: String) {
        _uiState.value = _uiState.value.copy(topic = value)
    }

    fun onToneChanged(value: String) {
        _uiState.value = _uiState.value.copy(tone = value)
    }

    fun generateScript() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = generateReelScriptUseCase(
                topic = _uiState.value.topic,
                tone = _uiState.value.tone,
                durationSeconds = _uiState.value.durationSeconds
            )) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, suggestion = result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                AppResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
}
