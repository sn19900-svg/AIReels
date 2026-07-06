package com.nabil.aireels.feature.scriptgen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ScriptGenScreen(
    onBack: () -> Unit,
    viewModel: ScriptGenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "توليد فكرة ونص الريلز")
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
            label = { Text("الأسلوب (حماسي، هادئ، ساخر...)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.generateScript() },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (uiState.isLoading) "جاري التوليد..." else "توليد النص")
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message)
        }

        uiState.suggestion?.let { suggestion ->
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "الجملة الافتتاحية:")
            Text(text = suggestion.hook)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "النص الكامل:")
            Text(text = suggestion.fullScript)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "الترجمات المقترحة:")
            suggestion.captions.forEach { caption -> Text(text = "- $caption") }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "الوسوم:")
            Text(text = suggestion.hashtags.joinToString(" "))
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = "رجوع")
        }
    }
}
