package com.nabil.aireels.feature.autoreel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nabil.aireels.domain.model.MediaSourceMode
import java.io.File

@Composable
fun AutoReelScreen(
    onBack: () -> Unit,
    onNavigateToExport: () -> Unit,
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

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onAudioSelected(context, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "توليد ريلز تلقائي احترافي")
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

        Text(text = "مصدر الصور/المشاهد:")
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = uiState.mediaSourceMode == MediaSourceMode.USER_PHOTOS,
                onClick = { viewModel.onMediaSourceModeChanged(MediaSourceMode.USER_PHOTOS) }
            )
            Text(text = "صوري الخاصة")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = uiState.mediaSourceMode == MediaSourceMode.AI_STOCK_PHOTOS,
                onClick = { viewModel.onMediaSourceModeChanged(MediaSourceMode.AI_STOCK_PHOTOS) }
            )
            Text(text = "صور واقعية تلقائية (مجانية)")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = uiState.mediaSourceMode == MediaSourceMode.AI_STOCK_VIDEO,
                onClick = { viewModel.onMediaSourceModeChanged(MediaSourceMode.AI_STOCK_VIDEO) }
            )
            Text(text = "مقاطع فيديو سينمائية تلقائية (مجانية)")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = uiState.mediaSourceMode == MediaSourceMode.HYBRID_HERO_PLUS_AI,
                onClick = { viewModel.onMediaSourceModeChanged(MediaSourceMode.HYBRID_HERO_PLUS_AI) }
            )
            Text(text = "صورتي كلقطة ختامية + مشاهد AI تمهيدية")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.mediaSourceMode == MediaSourceMode.USER_PHOTOS ||
            uiState.mediaSourceMode == MediaSourceMode.HYBRID_HERO_PLUS_AI
        ) {
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
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "تفعيل الترجمة التلقائية على الفيديو")
            Switch(
                checked = uiState.captionsEnabled,
                onCheckedChange = viewModel::onCaptionsEnabledChanged
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "إضافة صوت من قبلي (موسيقى أو تعليق)")
            Switch(
                checked = uiState.audioEnabled,
                onCheckedChange = viewModel::onAudioEnabledChanged
            )
        }

        if (uiState.audioEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { audioPickerLauncher.launch("audio/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.selectedAudioPath != null) {
                        "تم اختيار ملف صوتي ✓ (اضغط للتغيير)"
                    } else {
                        "اختيار ملف صوتي من الجهاز"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val workingDir = File(context.getExternalFilesDir(null), "autoreel").apply { mkdirs() }
                viewModel.generateAutoReel(workingDir)
            },
            enabled = ((uiState.mediaSourceMode != MediaSourceMode.USER_PHOTOS && uiState.mediaSourceMode != MediaSourceMode.HYBRID_HERO_PLUS_AI) || uiState.selectedImagePaths.isNotEmpty()) &&
                uiState.topic.isNotBlank() &&
                !uiState.isProcessing &&
                (!uiState.audioEnabled || uiState.selectedAudioPath != null),
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
            Text(text = "تم إنشاء الريلز بنجاح!")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToExport, modifier = Modifier.fillMaxWidth()) {
                Text(text = "الانتقال إلى شاشة التصدير وحفظ الفيديو")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = "رجوع")
        }
    }
}
