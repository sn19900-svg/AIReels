package com.nabil.aireels.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@Composable
fun EditorScreen(
    onNavigateToCaptions: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "محرر الفيديو")
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(uiState.clips) { clip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = File(clip.filePath).name)
                    TextButton(onClick = { viewModel.removeClip(clip.id) }) {
                        Text(text = "حذف")
                    }
                }
            }
        }

        uiState.errorMessage?.let { message ->
            Text(text = message)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val outputDir = File(context.getExternalFilesDir(null), "merged").apply { mkdirs() }
                viewModel.mergeClips(outputDir)
            },
            enabled = uiState.clips.isNotEmpty() && !uiState.isMerging,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (uiState.isMerging) "جاري الدمج..." else "دمج المقاطع")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToCaptions,
            enabled = uiState.mergedVideoPath != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "متابعة إلى الترجمة التلقائية")
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = "رجوع")
        }
    }
}
