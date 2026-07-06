#!/usr/bin/env bash
set -e

echo "==> بدء إنشاء مشروع AI Reels ..."

# ---------------------------------------------------------------------------
# 1) هيكل المجلدات
# ---------------------------------------------------------------------------
mkdir -p .github/workflows
mkdir -p app/src/main/assets
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/mipmap-anydpi-v26
mkdir -p app/src/main/java/com/nabil/aireels/core/navigation
mkdir -p app/src/main/java/com/nabil/aireels/core/ui/theme
mkdir -p app/src/main/java/com/nabil/aireels/core/util
mkdir -p app/src/main/java/com/nabil/aireels/di
mkdir -p app/src/main/java/com/nabil/aireels/domain/model
mkdir -p app/src/main/java/com/nabil/aireels/domain/repository
mkdir -p app/src/main/java/com/nabil/aireels/domain/usecase
mkdir -p app/src/main/java/com/nabil/aireels/data/remote/gemini
mkdir -p app/src/main/java/com/nabil/aireels/data/repository
mkdir -p app/src/main/java/com/nabil/aireels/data/speech
mkdir -p app/src/main/java/com/nabil/aireels/data/state
mkdir -p app/src/main/java/com/nabil/aireels/feature/home
mkdir -p app/src/main/java/com/nabil/aireels/feature/camera
mkdir -p app/src/main/java/com/nabil/aireels/feature/scriptgen
mkdir -p app/src/main/java/com/nabil/aireels/feature/editor
mkdir -p app/src/main/java/com/nabil/aireels/feature/captions
mkdir -p app/src/main/java/com/nabil/aireels/feature/export

# ---------------------------------------------------------------------------
# 2) ملفات جذر المشروع
# ---------------------------------------------------------------------------

cat << 'EOF' > settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AIReels"
include(":app")
EOF

cat << 'EOF' > build.gradle.kts
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
EOF

cat << 'EOF' > gradle.properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.parallel=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
EOF

cat << 'EOF' > local.properties.example
sdk.dir=/path/to/android/sdk
gemini.api.key=PUT_YOUR_FREE_GEMINI_API_KEY_HERE
EOF

cat << 'EOF' > .gitignore
*.iml
.gradle
/local.properties
.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
*.apk
*.aab
EOF

# ---------------------------------------------------------------------------
# 3) GitHub Actions Workflow
# ---------------------------------------------------------------------------

cat << 'EOF' > .github/workflows/android.yml
name: Android CI Build

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Inject Gemini API Key into local.properties
        env:
          GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
        run: |
          echo "gemini.api.key=${GEMINI_API_KEY}" >> local.properties

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 8.7

      - name: Build Debug APK
        run: gradle :app:assembleDebug --no-daemon --stacktrace

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: aireels-debug-apk
          path: app/build/outputs/apk/debug/*.apk
          if-no-files-found: error
EOF

# ---------------------------------------------------------------------------
# 4) app/build.gradle.kts
# ---------------------------------------------------------------------------

cat << 'EOF' > app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.nabil.aireels"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nabil.aireels"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = java.util.Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(localPropsFile.inputStream())
        }
        val geminiApiKey = localProps.getProperty("gemini.api.key")
            ?: System.getenv("GEMINI_API_KEY")
            ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-video:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    implementation("com.moizhassan.ffmpeg:ffmpeg-kit-16kb:6.1.1")

    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
EOF

cat << 'EOF' > app/proguard-rules.pro
# قواعد ProGuard الافتراضية - يمكن تخصيصها لاحقاً حسب الحاجة
-keep class com.nabil.aireels.** { *; }
-dontwarn com.arthenica.**
-keep class com.arthenica.** { *; }
-keep class org.tensorflow.** { *; }
EOF

# ---------------------------------------------------------------------------
# 5) AndroidManifest.xml
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <uses-feature android:name="android.hardware.camera" android:required="true" />
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />

    <application
        android:name=".AiReelsApplication"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@style/Theme.AIReels"
        android:supportsRtl="true"
        android:requestLegacyExternalStorage="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.AIReels">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
EOF

cat << 'EOF' > app/src/main/res/values/strings.xml
<resources>
    <string name="app_name">AI Reels</string>
</resources>
EOF

cat << 'EOF' > app/src/main/res/values/themes.xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.AIReels" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">@android:color/black</item>
    </style>
</resources>
EOF

cat << 'EOF' > app/src/main/assets/PUT_WHISPER_MODEL_HERE.txt
هذا المجلد يجب أن يحتوي على ملفين حتى تعمل ميزة الترجمة التلقائية (Whisper):

1) whisper_tiny.tflite
   - نموذج Whisper المحوّل إلى TensorFlow Lite (نسخة tiny أو base خفيفة).
   - يمكن تحميله من مستودعات مثل usefulsensors/openai-whisper على GitHub
     (قسم Releases فيه ملفات .tflite جاهزة).

2) whisper_vocab.json
   - ملف يربط رقم كل Token بالنص المقابل له (Tokenizer Vocabulary).
   - يُستخرج عادة مع نفس النموذج أو من مستودع openai/whisper الأصلي.

ضع الملفين هنا بنفس الاسمين تماماً، ثم أعد بناء المشروع.
لاحظ: قد تحتاج لتعديل بسيط في WhisperInterpreterHelper.kt حسب الشكل
الدقيق لمدخلات/مخرجات النموذج الذي تختاره، لأنها تختلف قليلاً بين التحويلات المختلفة.
EOF

# ---------------------------------------------------------------------------
# 6) Application + MainActivity
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/AiReelsApplication.kt
package com.nabil.aireels

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AiReelsApplication : Application()
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/MainActivity.kt
package com.nabil.aireels

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nabil.aireels.core.navigation.AiReelsNavGraph
import com.nabil.aireels.core.ui.theme.AIReelsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AIReelsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AiReelsNavGraph()
                }
            }
        }
    }
}
EOF

# ---------------------------------------------------------------------------
# 7) Navigation
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/core/navigation/Screen.kt
package com.nabil.aireels.core.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object ScriptGen : Screen("script_gen")
    data object Editor : Screen("editor")
    data object Captions : Screen("captions")
    data object Export : Screen("export")
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/core/navigation/NavGraph.kt
package com.nabil.aireels.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nabil.aireels.feature.camera.CameraScreen
import com.nabil.aireels.feature.captions.CaptionsScreen
import com.nabil.aireels.feature.editor.EditorScreen
import com.nabil.aireels.feature.export.ExportScreen
import com.nabil.aireels.feature.home.HomeScreen
import com.nabil.aireels.feature.scriptgen.ScriptGenScreen

@Composable
fun AiReelsNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToScriptGen = { navController.navigate(Screen.ScriptGen.route) },
                onNavigateToEditor = { navController.navigate(Screen.Editor.route) }
            )
        }
        composable(Screen.Camera.route) {
            CameraScreen(onClipsReady = { navController.navigate(Screen.Editor.route) })
        }
        composable(Screen.ScriptGen.route) {
            ScriptGenScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Editor.route) {
            EditorScreen(
                onNavigateToCaptions = { navController.navigate(Screen.Captions.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Captions.route) {
            CaptionsScreen(onNavigateToExport = { navController.navigate(Screen.Export.route) })
        }
        composable(Screen.Export.route) {
            ExportScreen(onBack = { navController.popBackStack(Screen.Home.route, false) })
        }
    }
}
EOF

# ---------------------------------------------------------------------------
# 8) Theme
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/core/ui/theme/Color.kt
package com.nabil.aireels.core.ui.theme

import androidx.compose.ui.graphics.Color

val PrimaryPurple = Color(0xFF6C4AB6)
val PrimaryPurpleDark = Color(0xFF4A2F87)
val AccentPink = Color(0xFFE94F8C)
val SurfaceDark = Color(0xFF121212)
val OnSurfaceLight = Color(0xFFF5F5F5)
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/core/ui/theme/Type.kt
package com.nabil.aireels.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
)
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/core/ui/theme/Theme.kt
package com.nabil.aireels.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = PrimaryPurple,
    secondary = AccentPink,
    background = SurfaceDark,
    surface = SurfaceDark,
    onPrimary = OnSurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight
)

@Composable
fun AIReelsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
EOF

# ---------------------------------------------------------------------------
# 9) Utilities
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/core/util/Constants.kt
package com.nabil.aireels.core.util

object Constants {
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    const val GEMINI_MODEL = "gemini-2.0-flash"
    const val WHISPER_MODEL_ASSET = "whisper_tiny.tflite"
    const val WHISPER_VOCAB_ASSET = "whisper_vocab.json"
    const val SAMPLE_RATE = 16000
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/core/util/AppResult.kt
package com.nabil.aireels.core.util

sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()
}
EOF

# ---------------------------------------------------------------------------
# 10) Domain - Models
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/model/Clip.kt
package com.nabil.aireels.domain.model

data class Clip(
    val id: String,
    val filePath: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs
)
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/model/ScriptSuggestion.kt
package com.nabil.aireels.domain.model

data class ScriptSuggestion(
    val hook: String,
    val fullScript: String,
    val captions: List<String>,
    val hashtags: List<String>
)
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/model/TranscriptSegment.kt
package com.nabil.aireels.domain.model

data class TranscriptSegment(
    val text: String,
    val startMs: Long,
    val endMs: Long
)
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/model/ReelProject.kt
package com.nabil.aireels.domain.model

data class ReelProject(
    val id: String,
    val title: String,
    val clips: List<Clip> = emptyList(),
    val backgroundMusicPath: String? = null,
    val mergedVideoPath: String? = null,
    val script: ScriptSuggestion? = null,
    val transcriptSegments: List<TranscriptSegment> = emptyList()
)
EOF

# ---------------------------------------------------------------------------
# 11) Domain - Repository Interfaces
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/repository/GeminiRepository.kt
package com.nabil.aireels.domain.repository

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.ScriptSuggestion

interface GeminiRepository {
    suspend fun generateReelScript(topic: String, tone: String, durationSeconds: Int): AppResult<ScriptSuggestion>
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/repository/VideoRepository.kt
package com.nabil.aireels.domain.repository

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.Clip

interface VideoRepository {
    suspend fun trimClip(inputPath: String, startMs: Long, endMs: Long, outputPath: String): AppResult<String>
    suspend fun mergeClips(clips: List<Clip>, outputPath: String): AppResult<String>
    suspend fun addAudioTrack(videoPath: String, audioPath: String, outputPath: String): AppResult<String>
    suspend fun extractAudio(videoPath: String, outputPath: String): AppResult<String>
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/repository/TranscriptionRepository.kt
package com.nabil.aireels.domain.repository

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.TranscriptSegment

interface TranscriptionRepository {
    suspend fun transcribeAudio(pcmAudioPath: String): AppResult<List<TranscriptSegment>>
}
EOF

# ---------------------------------------------------------------------------
# 12) Domain - Use Cases
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/usecase/GenerateReelScriptUseCase.kt
package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.ScriptSuggestion
import com.nabil.aireels.domain.repository.GeminiRepository
import javax.inject.Inject

class GenerateReelScriptUseCase @Inject constructor(
    private val geminiRepository: GeminiRepository
) {
    suspend operator fun invoke(topic: String, tone: String, durationSeconds: Int): AppResult<ScriptSuggestion> {
        if (topic.isBlank()) {
            return AppResult.Error("الرجاء إدخال موضوع الريلز أولاً")
        }
        return geminiRepository.generateReelScript(topic, tone, durationSeconds)
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/usecase/TrimClipUseCase.kt
package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.repository.VideoRepository
import javax.inject.Inject

class TrimClipUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(inputPath: String, startMs: Long, endMs: Long, outputPath: String): AppResult<String> {
        if (endMs <= startMs) {
            return AppResult.Error("زمن النهاية يجب أن يكون أكبر من زمن البداية")
        }
        return videoRepository.trimClip(inputPath, startMs, endMs, outputPath)
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/usecase/MergeClipsUseCase.kt
package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.Clip
import com.nabil.aireels.domain.repository.VideoRepository
import javax.inject.Inject

class MergeClipsUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(clips: List<Clip>, outputPath: String): AppResult<String> {
        if (clips.isEmpty()) {
            return AppResult.Error("لا توجد مقاطع فيديو لدمجها")
        }
        return videoRepository.mergeClips(clips, outputPath)
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/domain/usecase/TranscribeAudioUseCase.kt
package com.nabil.aireels.domain.usecase

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.TranscriptSegment
import com.nabil.aireels.domain.repository.TranscriptionRepository
import javax.inject.Inject

class TranscribeAudioUseCase @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository
) {
    suspend operator fun invoke(pcmAudioPath: String): AppResult<List<TranscriptSegment>> {
        return transcriptionRepository.transcribeAudio(pcmAudioPath)
    }
}
EOF

# ---------------------------------------------------------------------------
# 13) Data - Gemini Remote (REST مباشر - مجاني عبر مفتاح AI Studio)
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/data/remote/gemini/GeminiModels.kt
package com.nabil.aireels.data.remote.gemini

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.9,
    val topK: Int = 40,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 1024
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null
)
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/data/remote/gemini/GeminiApiService.kt
package com.nabil.aireels.data.remote.gemini

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
EOF

# ---------------------------------------------------------------------------
# 14) Data - State Holder المشترك بين الشاشات
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/data/state/ProjectStateHolder.kt
package com.nabil.aireels.data.state

import com.nabil.aireels.domain.model.ReelProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectStateHolder @Inject constructor() {

    private val _currentProject = MutableStateFlow(
        ReelProject(id = UUID.randomUUID().toString(), title = "مشروع بدون عنوان")
    )
    val currentProject: StateFlow<ReelProject> = _currentProject

    fun updateProject(transform: (ReelProject) -> ReelProject) {
        _currentProject.value = transform(_currentProject.value)
    }

    fun resetProject() {
        _currentProject.value = ReelProject(id = UUID.randomUUID().toString(), title = "مشروع بدون عنوان")
    }
}
EOF

# ---------------------------------------------------------------------------
# 15) Data - Repositories Implementation
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/data/repository/GeminiRepositoryImpl.kt
package com.nabil.aireels.data.repository

import com.nabil.aireels.BuildConfig
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.core.util.Constants
import com.nabil.aireels.data.remote.gemini.GeminiApiService
import com.nabil.aireels.data.remote.gemini.GeminiContent
import com.nabil.aireels.data.remote.gemini.GeminiPart
import com.nabil.aireels.data.remote.gemini.GeminiRequest
import com.nabil.aireels.domain.model.ScriptSuggestion
import com.nabil.aireels.domain.repository.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class GeminiRepositoryImpl @Inject constructor(
    private val geminiApiService: GeminiApiService
) : GeminiRepository {

    override suspend fun generateReelScript(
        topic: String,
        tone: String,
        durationSeconds: Int
    ): AppResult<ScriptSuggestion> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                return@withContext AppResult.Error(
                    "مفتاح Gemini API غير موجود. أضفه في local.properties أو GitHub Secrets"
                )
            }

            val prompt = buildPrompt(topic, tone, durationSeconds)
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
            )

            val response = geminiApiService.generateContent(
                model = Constants.GEMINI_MODEL,
                apiKey = apiKey,
                request = request
            )

            val rawText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: return@withContext AppResult.Error("لم يتم استلام رد من Gemini")

            val cleanedJson = extractJson(rawText)
            val json = JSONObject(cleanedJson)

            val captionsArray = json.optJSONArray("captions")
            val captions = mutableListOf<String>()
            if (captionsArray != null) {
                for (i in 0 until captionsArray.length()) {
                    captions.add(captionsArray.getString(i))
                }
            }

            val hashtagsArray = json.optJSONArray("hashtags")
            val hashtags = mutableListOf<String>()
            if (hashtagsArray != null) {
                for (i in 0 until hashtagsArray.length()) {
                    hashtags.add(hashtagsArray.getString(i))
                }
            }

            val suggestion = ScriptSuggestion(
                hook = json.optString("hook", ""),
                fullScript = json.optString("full_script", ""),
                captions = captions,
                hashtags = hashtags
            )

            AppResult.Success(suggestion)
        } catch (e: Exception) {
            AppResult.Error("فشل توليد النص: ${e.message}", e)
        }
    }

    private fun buildPrompt(topic: String, tone: String, durationSeconds: Int): String {
        return """
            أنت كاتب محتوى محترف لمنصات الفيديو القصيرة (ريلز/شورتس).
            الموضوع: $topic
            الأسلوب المطلوب: $tone
            مدة الفيديو المستهدفة بالثواني: $durationSeconds

            أعطني الناتج بصيغة JSON فقط وبدون أي نص إضافي قبله أو بعده، بالمخطط التالي بالضبط:
            {
              "hook": "جملة افتتاحية قوية لجذب المشاهد في أول 3 ثواني",
              "full_script": "النص الكامل للفيديو مقسم بفقرات قصيرة",
              "captions": ["سطر ترجمة 1", "سطر ترجمة 2", "سطر ترجمة 3"],
              "hashtags": ["#وسم1", "#وسم2", "#وسم3"]
            }
        """.trimIndent()
    }

    private fun extractJson(rawText: String): String {
        val start = rawText.indexOf('{')
        val end = rawText.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) {
            rawText.substring(start, end + 1)
        } else {
            rawText
        }
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/data/repository/FfmpegVideoRepositoryImpl.kt
package com.nabil.aireels.data.repository

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.Clip
import com.nabil.aireels.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FfmpegVideoRepositoryImpl @Inject constructor() : VideoRepository {

    override suspend fun trimClip(
        inputPath: String,
        startMs: Long,
        endMs: Long,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val startSeconds = startMs / 1000.0
        val durationSeconds = (endMs - startMs) / 1000.0
        val command = "-y -i \"$inputPath\" -ss $startSeconds -t $durationSeconds " +
            "-c:v mpeg4 -c:a aac \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل قص المقطع: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun mergeClips(
        clips: List<Clip>,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        if (clips.isEmpty()) {
            return@withContext AppResult.Error("لا توجد مقاطع للدمج")
        }

        val listFile = File(outputPath).parentFile?.resolve("concat_list.txt")
            ?: return@withContext AppResult.Error("مسار الإخراج غير صالح")

        listFile.bufferedWriter().use { writer ->
            clips.forEach { clip ->
                writer.write("file '${clip.filePath}'")
                writer.newLine()
            }
        }

        val command = "-y -f concat -safe 0 -i \"${listFile.absolutePath}\" " +
            "-c:v mpeg4 -c:a aac \"$outputPath\""

        val session = FFmpegKit.execute(command)
        listFile.delete()

        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل دمج المقاطع: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun addAudioTrack(
        videoPath: String,
        audioPath: String,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val command = "-y -i \"$videoPath\" -i \"$audioPath\" " +
            "-c:v copy -c:a aac -map 0:v:0 -map 1:a:0 -shortest \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل إضافة المسار الصوتي: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    override suspend fun extractAudio(
        videoPath: String,
        outputPath: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val command = "-y -i \"$videoPath\" -vn -ac 1 -ar 16000 -f wav \"$outputPath\""

        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            AppResult.Success(outputPath)
        } else {
            AppResult.Error("فشل استخراج الصوت: ${session.failStackTrace ?: session.allLogsAsString}")
        }
    }
}
EOF

# ---------------------------------------------------------------------------
# 16) Data - Speech (Whisper TFLite)
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/data/speech/MelSpectrogramProcessor.kt
package com.nabil.aireels.data.speech

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class MelSpectrogramProcessor(
    private val sampleRate: Int = 16000,
    private val winLength: Int = 400,
    private val nFft: Int = 512,
    private val hopLength: Int = 160,
    private val nMels: Int = 80
) {

    fun process(audioSamples: FloatArray): Array<FloatArray> {
        val window = hannWindow(winLength)
        val melFilterBank = createMelFilterBank()
        val frames = mutableListOf<FloatArray>()

        var offset = 0
        while (offset + winLength <= audioSamples.size) {
            val frame = FloatArray(nFft)
            for (i in 0 until winLength) {
                frame[i] = audioSamples[offset + i] * window[i]
            }

            val (real, imag) = fft(frame)
            val numBins = nFft / 2 + 1
            val powerSpectrum = FloatArray(numBins)
            for (i in 0 until numBins) {
                powerSpectrum[i] = real[i] * real[i] + imag[i] * imag[i]
            }

            val melEnergies = FloatArray(nMels)
            for (m in 0 until nMels) {
                var sum = 0.0f
                for (k in 0 until numBins) {
                    sum += powerSpectrum[k] * melFilterBank[m][k]
                }
                melEnergies[m] = ln(max(sum, 1e-10f))
            }
            frames.add(melEnergies)
            offset += hopLength
        }
        return frames.toTypedArray()
    }

    private fun hannWindow(size: Int): FloatArray {
        return FloatArray(size) { i ->
            (0.5 * (1.0 - cos(2.0 * PI * i / (size - 1)))).toFloat()
        }
    }

    private fun fft(input: FloatArray): Pair<FloatArray, FloatArray> {
        val real = input.copyOf()
        val imag = FloatArray(input.size)
        fftInPlace(real, imag)
        return Pair(real, imag)
    }

    private fun fftInPlace(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n <= 1) return

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tempReal = real[i]; real[i] = real[j]; real[j] = tempReal
                val tempImag = imag[i]; imag[i] = imag[j]; imag[j] = tempImag
            }
        }

        var length = 2
        while (length <= n) {
            val angle = -2.0 * PI / length
            val wReal = cos(angle).toFloat()
            val wImag = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var curWReal = 1.0f
                var curWImag = 0.0f
                for (k in 0 until length / 2) {
                    val evenIndex = i + k
                    val oddIndex = i + k + length / 2
                    val evenReal = real[evenIndex]
                    val evenImag = imag[evenIndex]
                    val oddReal = real[oddIndex] * curWReal - imag[oddIndex] * curWImag
                    val oddImag = real[oddIndex] * curWImag + imag[oddIndex] * curWReal
                    real[evenIndex] = evenReal + oddReal
                    imag[evenIndex] = evenImag + oddImag
                    real[oddIndex] = evenReal - oddReal
                    imag[oddIndex] = evenImag - oddImag
                    val nextWReal = curWReal * wReal - curWImag * wImag
                    val nextWImag = curWReal * wImag + curWImag * wReal
                    curWReal = nextWReal
                    curWImag = nextWImag
                }
                i += length
            }
            length = length shl 1
        }
    }

    private fun createMelFilterBank(): Array<FloatArray> {
        val numFftBins = nFft / 2 + 1
        val filterBank = Array(nMels) { FloatArray(numFftBins) }

        val melMin = hzToMel(0.0)
        val melMax = hzToMel(sampleRate / 2.0)
        val melPoints = DoubleArray(nMels + 2) { i ->
            melMin + i * (melMax - melMin) / (nMels + 1)
        }
        val hzPoints = melPoints.map { melToHz(it) }
        val binPoints = hzPoints.map { floor((nFft + 1) * it / sampleRate).toInt() }

        for (m in 1..nMels) {
            val left = binPoints[m - 1]
            val center = binPoints[m]
            val right = binPoints[m + 1]

            if (center != left) {
                for (k in left until center) {
                    if (k in 0 until numFftBins) {
                        filterBank[m - 1][k] = (k - left).toFloat() / (center - left).toFloat()
                    }
                }
            }
            if (right != center) {
                for (k in center until right) {
                    if (k in 0 until numFftBins) {
                        filterBank[m - 1][k] = (right - k).toFloat() / (right - center).toFloat()
                    }
                }
            }
        }
        return filterBank
    }

    private fun hzToMel(hz: Double): Double = 2595.0 * log10(1.0 + hz / 700.0)
    private fun melToHz(mel: Double): Double = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/data/speech/WhisperInterpreterHelper.kt
package com.nabil.aireels.data.speech

import android.content.Context
import com.nabil.aireels.core.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperInterpreterHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var vocabMap: Map<Int, String>? = null
    private val melProcessor = MelSpectrogramProcessor()

    private fun ensureLoaded() {
        if (interpreter == null) {
            interpreter = Interpreter(loadModelFile(Constants.WHISPER_MODEL_ASSET))
        }
        if (vocabMap == null) {
            vocabMap = loadVocab(Constants.WHISPER_VOCAB_ASSET)
        }
    }

    fun transcribe(audioSamples: FloatArray): String {
        ensureLoaded()
        val currentInterpreter = interpreter
            ?: throw IllegalStateException("لم يتم تحميل نموذج Whisper بنجاح")

        val melFrames = melProcessor.process(audioSamples)
        val nMels = 80
        val nFrames = melFrames.size

        val inputBuffer = Array(1) { Array(nMels) { FloatArray(nFrames) } }
        for (t in 0 until nFrames) {
            for (m in 0 until nMels) {
                inputBuffer[0][m][t] = melFrames[t][m]
            }
        }

        val maxTokens = 224
        val outputTokens = Array(1) { IntArray(maxTokens) }

        currentInterpreter.run(inputBuffer, outputTokens)

        return decodeTokens(outputTokens[0])
    }

    private fun decodeTokens(tokenIds: IntArray): String {
        val map = vocabMap ?: return ""
        val builder = StringBuilder()
        for (id in tokenIds) {
            if (id <= 0) break
            val token = map[id] ?: continue
            if (token.startsWith("<|") && token.endsWith("|>")) continue
            builder.append(token.replace("\u0120", " "))
        }
        return builder.toString().trim()
    }

    private fun loadModelFile(assetName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(assetName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadVocab(assetName: String): Map<Int, String> {
        val jsonText = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val json = JSONObject(jsonText)
        val map = mutableMapOf<Int, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key.toInt()] = json.getString(key)
        }
        return map
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/data/repository/WhisperTranscriptionRepositoryImpl.kt
package com.nabil.aireels.data.repository

import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.speech.WhisperInterpreterHelper
import com.nabil.aireels.domain.model.TranscriptSegment
import com.nabil.aireels.domain.repository.TranscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject

class WhisperTranscriptionRepositoryImpl @Inject constructor(
    private val whisperInterpreterHelper: WhisperInterpreterHelper
) : TranscriptionRepository {

    private val sampleRate = 16000
    private val chunkDurationSeconds = 30
    private val chunkSizeSamples = sampleRate * chunkDurationSeconds

    override suspend fun transcribeAudio(pcmAudioPath: String): AppResult<List<TranscriptSegment>> =
        withContext(Dispatchers.Default) {
            try {
                val file = File(pcmAudioPath)
                if (!file.exists()) {
                    return@withContext AppResult.Error("ملف الصوت غير موجود: $pcmAudioPath")
                }

                val samples = readWavAsFloatArray(file)
                val segments = mutableListOf<TranscriptSegment>()

                var offset = 0
                while (offset < samples.size) {
                    val end = minOf(offset + chunkSizeSamples, samples.size)
                    val chunk = samples.copyOfRange(offset, end)
                    val text = whisperInterpreterHelper.transcribe(chunk)

                    if (text.isNotBlank()) {
                        val startMs = (offset.toLong() * 1000L) / sampleRate
                        val endMs = (end.toLong() * 1000L) / sampleRate
                        segments.add(TranscriptSegment(text = text, startMs = startMs, endMs = endMs))
                    }
                    offset += chunkSizeSamples
                }

                AppResult.Success(segments)
            } catch (e: Exception) {
                AppResult.Error("فشل تحويل الصوت إلى نص: ${e.message}", e)
            }
        }

    private fun readWavAsFloatArray(file: File): FloatArray {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(44)
            raf.readFully(header)
            val dataSize = raf.length().toInt() - 44
            val pcmBytes = ByteArray(dataSize)
            raf.readFully(pcmBytes)

            val sampleCount = dataSize / 2
            val floatSamples = FloatArray(sampleCount)
            for (i in 0 until sampleCount) {
                val low = pcmBytes[i * 2].toInt() and 0xFF
                val high = pcmBytes[i * 2 + 1].toInt()
                val sampleValue = (high shl 8) or low
                floatSamples[i] = sampleValue.toShort().toFloat() / 32768.0f
            }
            return floatSamples
        }
    }
}
EOF

# ---------------------------------------------------------------------------
# 17) DI Modules (Hilt)
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/di/NetworkModule.kt
package com.nabil.aireels.di

import com.nabil.aireels.core.util.Constants
import com.nabil.aireels.data.remote.gemini.GeminiApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(retrofit: Retrofit): GeminiApiService {
        return retrofit.create(GeminiApiService::class.java)
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/di/RepositoryModule.kt
package com.nabil.aireels.di

import com.nabil.aireels.data.repository.FfmpegVideoRepositoryImpl
import com.nabil.aireels.data.repository.GeminiRepositoryImpl
import com.nabil.aireels.data.repository.WhisperTranscriptionRepositoryImpl
import com.nabil.aireels.domain.repository.GeminiRepository
import com.nabil.aireels.domain.repository.TranscriptionRepository
import com.nabil.aireels.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGeminiRepository(impl: GeminiRepositoryImpl): GeminiRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: FfmpegVideoRepositoryImpl): VideoRepository

    @Binds
    @Singleton
    abstract fun bindTranscriptionRepository(impl: WhisperTranscriptionRepositoryImpl): TranscriptionRepository
}
EOF

# ---------------------------------------------------------------------------
# 18) Feature: Home
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/home/HomeViewModel.kt
package com.nabil.aireels.feature.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel()
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/home/HomeScreen.kt
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
EOF

# ---------------------------------------------------------------------------
# 19) Feature: Camera
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/camera/CameraXController.kt
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
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/camera/CameraViewModel.kt
package com.nabil.aireels.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabil.aireels.data.state.ProjectStateHolder
import com.nabil.aireels.domain.model.Clip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

data class CameraUiState(
    val isRecording: Boolean = false,
    val recordedClipsCount: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val projectStateHolder: ProjectStateHolder
) : ViewModel() {

    private val _localState = MutableStateFlow(CameraUiState())

    val uiState: StateFlow<CameraUiState> = combine(
        _localState,
        projectStateHolder.currentProject
    ) { local, project ->
        local.copy(recordedClipsCount = project.clips.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CameraUiState())

    fun onRecordingStarted() {
        _localState.value = _localState.value.copy(isRecording = true, errorMessage = null)
    }

    fun onRecordingFinished(filePath: String) {
        val durationMs = estimateDurationMs(filePath)
        val newClip = Clip(
            id = UUID.randomUUID().toString(),
            filePath = filePath,
            durationMs = durationMs,
            trimStartMs = 0L,
            trimEndMs = durationMs
        )
        projectStateHolder.updateProject { project ->
            project.copy(clips = project.clips + newClip)
        }
        _localState.value = _localState.value.copy(isRecording = false)
    }

    fun onRecordingError(message: String) {
        _localState.value = _localState.value.copy(isRecording = false, errorMessage = message)
    }

    private fun estimateDurationMs(filePath: String): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val duration = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/camera/CameraScreen.kt
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
EOF

# ---------------------------------------------------------------------------
# 20) Feature: Script Generation (Gemini)
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/scriptgen/ScriptGenViewModel.kt
package com.nabil.aireels.feature.scriptgen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.domain.model.ScriptSuggestion
import com.nabil.aireels.domain.usecase.GenerateReelScriptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScriptGenUiState(
    val topic: String = "",
    val tone: String = "حماسي",
    val durationSeconds: Int = 30,
    val isLoading: Boolean = false,
    val suggestion: ScriptSuggestion? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ScriptGenViewModel @Inject constructor(
    private val generateReelScriptUseCase: GenerateReelScriptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptGenUiState())
    val uiState: StateFlow<ScriptGenUiState> = _uiState

    fun onTopicChanged(value: String) {
        _uiState.value = _uiState.value.copy(topic = value)
    }

    fun onToneChanged(value: String) {
        _uiState.value = _uiState.value.copy(tone = value)
    }

    fun generateScript() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = generateReelScriptUseCase(
                topic = _uiState.value.topic,
                tone = _uiState.value.tone,
                durationSeconds = _uiState.value.durationSeconds
            )) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, suggestion = result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                AppResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/scriptgen/ScriptGenScreen.kt
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
EOF

# ---------------------------------------------------------------------------
# 21) Feature: Editor (FFmpeg)
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/editor/EditorViewModel.kt
package com.nabil.aireels.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.state.ProjectStateHolder
import com.nabil.aireels.domain.model.Clip
import com.nabil.aireels.domain.usecase.MergeClipsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class EditorUiState(
    val clips: List<Clip> = emptyList(),
    val isMerging: Boolean = false,
    val mergedVideoPath: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val projectStateHolder: ProjectStateHolder,
    private val mergeClipsUseCase: MergeClipsUseCase
) : ViewModel() {

    private val _localState = MutableStateFlow(EditorUiState())

    val uiState: StateFlow<EditorUiState> = combine(
        _localState,
        projectStateHolder.currentProject
    ) { local, project ->
        local.copy(clips = project.clips, mergedVideoPath = project.mergedVideoPath)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EditorUiState())

    fun removeClip(clipId: String) {
        projectStateHolder.updateProject { project ->
            project.copy(clips = project.clips.filterNot { it.id == clipId })
        }
    }

    fun mergeClips(outputDir: File) {
        viewModelScope.launch {
            _localState.value = _localState.value.copy(isMerging = true, errorMessage = null)
            val currentClips = projectStateHolder.currentProject.value.clips
            val outputFile = File(outputDir, "merged_${System.currentTimeMillis()}.mp4")

            when (val result = mergeClipsUseCase(currentClips, outputFile.absolutePath)) {
                is AppResult.Success -> {
                    projectStateHolder.updateProject { it.copy(mergedVideoPath = result.data) }
                    _localState.value = _localState.value.copy(isMerging = false)
                }
                is AppResult.Error -> {
                    _localState.value = _localState.value.copy(isMerging = false, errorMessage = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/editor/EditorScreen.kt
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
EOF

# ---------------------------------------------------------------------------
# 22) Feature: Captions (Whisper)
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/captions/CaptionsViewModel.kt
package com.nabil.aireels.feature.captions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabil.aireels.core.util.AppResult
import com.nabil.aireels.data.state.ProjectStateHolder
import com.nabil.aireels.domain.repository.VideoRepository
import com.nabil.aireels.domain.usecase.TranscribeAudioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CaptionsUiState(
    val isProcessing: Boolean = false,
    val transcriptText: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class CaptionsViewModel @Inject constructor(
    private val projectStateHolder: ProjectStateHolder,
    private val videoRepository: VideoRepository,
    private val transcribeAudioUseCase: TranscribeAudioUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptionsUiState())
    val uiState: StateFlow<CaptionsUiState> = _uiState

    fun generateCaptions(workingDir: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)

            val mergedVideoPath = projectStateHolder.currentProject.value.mergedVideoPath
            if (mergedVideoPath == null) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "الرجاء دمج المقاطع أولاً في المحرر"
                )
                return@launch
            }

            val audioFile = File(workingDir, "extracted_audio.wav")
            when (val audioResult = videoRepository.extractAudio(mergedVideoPath, audioFile.absolutePath)) {
                is AppResult.Success -> {
                    when (val transcriptResult = transcribeAudioUseCase(audioResult.data)) {
                        is AppResult.Success -> {
                            val fullText = transcriptResult.data.joinToString("\n") { it.text }
                            projectStateHolder.updateProject { project ->
                                project.copy(transcriptSegments = transcriptResult.data)
                            }
                            _uiState.value = _uiState.value.copy(
                                isProcessing = false,
                                transcriptText = fullText
                            )
                        }
                        is AppResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isProcessing = false,
                                errorMessage = transcriptResult.message
                            )
                        }
                        AppResult.Loading -> Unit
                    }
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(isProcessing = false, errorMessage = audioResult.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/captions/CaptionsScreen.kt
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
EOF

# ---------------------------------------------------------------------------
# 23) Feature: Export
# ---------------------------------------------------------------------------

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/export/ExportViewModel.kt
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
EOF

cat << 'EOF' > app/src/main/java/com/nabil/aireels/feature/export/ExportScreen.kt
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
EOF

# ---------------------------------------------------------------------------
# 24) Git add & commit
# ---------------------------------------------------------------------------

echo "==> تمت كتابة جميع الملفات بنجاح."

if [ -d ".git" ]; then
  git add .
  git commit -m "Initial AI Reels App Commit"
  echo "==> تم عمل commit محلي بنجاح. الآن نفذ: git push"
else
  echo "==> تحذير: هذا المجلد ليس مستودع Git بعد. نفذ git init أولاً ثم أعد تشغيل السكربت، أو نفذ git add/commit يدوياً."
fi

echo "==> اكتمل إنشاء المشروع."
