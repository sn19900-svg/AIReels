package com.nabil.aireels.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nabil.aireels.feature.autoreel.AutoReelScreen
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
                onNavigateToEditor = { navController.navigate(Screen.Editor.route) },
                onNavigateToAutoReel = { navController.navigate(Screen.AutoReel.route) }
            )
        }
        composable(Screen.Camera.route) {
            CameraScreen(onClipsReady = { navController.navigate(Screen.Editor.route) })
        }
        composable(Screen.ScriptGen.route) {
            ScriptGenScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAutoReel = { navController.navigate(Screen.AutoReel.route) }
            )
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
        composable(Screen.AutoReel.route) {
            AutoReelScreen(
                onBack = { navController.popBackStack() },
                onNavigateToExport = { navController.navigate(Screen.Export.route) }
            )
        }
    }
}
