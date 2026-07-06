package com.nabil.aireels.feature.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXController(private val context: Context) {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun bindCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        onReady: () -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val qualitySelector = QualitySelector.from(
                Quality.HD,
                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
            )
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                videoCapture
            )
            onReady()
        }, ContextCompat.getMainExecutor(context))
    }

    fun startRecording(onFinished: (filePath: String) -> Unit, onError: (String) -> Unit) {
        val capture = videoCapture ?: run {
            onError("الكاميرا غير جاهزة بعد")
            return
        }

        val outputDir = File(context.getExternalFilesDir(null), "clips").apply { mkdirs() }
        val fileName = "clip_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val outputFile = File(outputDir, fileName)
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        activeRecording = capture.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    if (!event.hasError()) {
                        onFinished(outputFile.absolutePath)
                    } else {
                        onError("خطأ أثناء التسجيل: ${event.error}")
                    }
                }
            }
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun release() {
        cameraExecutor.shutdown()
    }
}
