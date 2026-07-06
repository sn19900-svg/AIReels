package com.nabil.aireels.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabil.aireels.data.state.ProjectStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ExportUiState(
    val mergedVideoPath: String? = null,
    val isExporting: Boolean = false,
    val exportedPath: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val projectStateHolder: ProjectStateHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ExportUiState(mergedVideoPath = projectStateHolder.currentProject.value.mergedVideoPath)
    )
    val uiState: StateFlow<ExportUiState> = _uiState

    fun exportToGallery(moviesDir: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null)
            val sourcePath = _uiState.value.mergedVideoPath
            if (sourcePath == null) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "لا يوجد فيديو مدمج للتصدير"
                )
                return@launch
            }

            try {
                val sourceFile = File(sourcePath)
                val destFile = File(moviesDir, "AIReels_${System.currentTimeMillis()}.mp4")
                sourceFile.copyTo(destFile, overwrite = true)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportedPath = destFile.absolutePath
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "فشل التصدير: ${e.message}"
                )
            }
        }
    }
}
