package com.nabil.aireels.core.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object ScriptGen : Screen("script_gen")
    data object Editor : Screen("editor")
    data object Captions : Screen("captions")
    data object Export : Screen("export")
}
