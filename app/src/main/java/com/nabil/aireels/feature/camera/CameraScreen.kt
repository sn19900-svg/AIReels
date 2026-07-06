package com.nabil.aireels.feature.camera

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CameraScreen(
    onClipsReady: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasAudioPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )
    }

    val cameraController = remember { CameraXController(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission && hasAudioPermission) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    cameraController.bindCamera(previewView, lifecycleOwner) {}
                    previewView
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "الرجاء منح صلاحيات الكاميرا والميكروفون")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                if (!uiState.isRecording) {
                    viewModel.onRecordingStarted()
                    cameraController.startRecording(
                        onFinished = { path -> viewModel.onRecordingFinished(path) },
                        onError = { message -> viewModel.onRecordingError(message) }
                    )
                } else {
                    cameraController.stopRecording()
                }
            }) {
                Text(text = if (uiState.isRecording) "إيقاف التسجيل" else "بدء التسجيل")
            }

            Button(
                onClick = onClipsReady,
                enabled = uiState.recordedClipsCount > 0
            ) {
                Text(text = "متابعة إلى المحرر (${uiState.recordedClipsCount} مقطع)")
            }
        }

        uiState.errorMessage?.let { message ->
            Text(text = message, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraController.release() }
    }
}
