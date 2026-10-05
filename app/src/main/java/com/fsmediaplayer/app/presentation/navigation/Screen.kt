package com.fsmediaplayer.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Player : Screen("player")
}
