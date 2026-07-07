package com.nabil.aireels.feature.autoreel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
fun AutoReelScreen(
    onBack: () -> Unit,
    viewModel: AutoReelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(context, uris)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "توليد ريلز تلقائي من صور")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.topic,
            onValueChange = viewModel::onTopicChanged,
            label = { Text("موضوع الريلز") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.tone,
            onValueChange = viewModel::onToneChanged,
            label = { Text("الأسلوب") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "اختيار الصور (${uiState.selectedImagePaths.size} محددة)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val workingDir = File(context.getExternalFilesDir(null), "autoreel").apply { mkdirs() }
                viewModel.generateAutoReel(workingDir)
            },
            enabled = uiState.selectedImagePaths.isNotEmpty() &&
                uiState.topic.isNotBlank() &&
                !uiState.isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (uiState.isProcessing) "جاري الإنشاء..." else "إنشاء الريلز تلقائياً")
        }

        if (uiState.isProcessing) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = uiState.progressMessage)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message)
        }

        uiState.resultVideoPath?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "تم إنشاء الريلز بنجاح! يمكنك الآن الانتقال لشاشة التصدير لحفظه في المعرض")
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = "رجوع")
        }
    }
}
