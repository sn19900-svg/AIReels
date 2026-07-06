package com.nabil.aireels.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToScriptGen: () -> Unit,
    onNavigateToEditor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "AI Reels")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateToCamera, modifier = Modifier.fillMaxWidth()) {
            Text(text = "تصوير مقطع جديد")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onNavigateToScriptGen, modifier = Modifier.fillMaxWidth()) {
            Text(text = "توليد فكرة ونص بالذكاء الاصطناعي")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onNavigateToEditor, modifier = Modifier.fillMaxWidth()) {
            Text(text = "الانتقال إلى المحرر")
        }
    }
}
