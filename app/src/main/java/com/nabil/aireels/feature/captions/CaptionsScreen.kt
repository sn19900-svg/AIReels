package com.nabil.aireels.feature.captions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@Composable
fun CaptionsScreen(
    onNavigateToExport: () -> Unit,
    viewModel: CaptionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "الترجمة التلقائية (Whisper محلي)")
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val workingDir = File(context.getExternalFilesDir(null), "audio").apply { mkdirs() }
                viewModel.generateCaptions(workingDir)
            },
            enabled = !uiState.isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (uiState.isProcessing) "جاري التفريغ الصوتي..." else "توليد الترجمة من الصوت")
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message)
        }

        if (uiState.transcriptText.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = uiState.transcriptText)
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onNavigateToExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "متابعة إلى التصدير")
        }
    }
}
