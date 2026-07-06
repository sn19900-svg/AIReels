package com.nabil.aireels.feature.export

import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "تصدير الريلز النهائي")
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                viewModel.exportToGallery(moviesDir)
            },
            enabled = uiState.mergedVideoPath != null && !uiState.isExporting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (uiState.isExporting) "جاري التصدير..." else "حفظ الفيديو في معرض الأجهزة")
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message)
        }

        uiState.exportedPath?.let { path ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "تم الحفظ في: $path")
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = "العودة إلى الرئيسية")
        }
    }
}
