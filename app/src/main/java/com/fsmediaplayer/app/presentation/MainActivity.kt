package com.fsmediaplayer.app.presentation

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fsmediaplayer.app.core.designsystem.theme.FSMediaPlayerTheme
import com.fsmediaplayer.app.core.player.FSPlayerManager
import com.fsmediaplayer.app.presentation.library.LibraryScreen
import com.fsmediaplayer.app.presentation.library.LibraryViewModel
import com.fsmediaplayer.app.presentation.navigation.Screen
import com.fsmediaplayer.app.presentation.player.PlayerScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerManager: FSPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            FSMediaPlayerTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Library.route
                ) {
                    composable(Screen.Library.route) {
                        val libraryViewModel: LibraryViewModel = hiltViewModel()
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            onVideoClick = { video ->
                                playerManager.playMedia(video.contentUri, video.title)
                                navController.navigate(Screen.Player.route)
                            }
                        )
                    }

                    composable(Screen.Player.route) {
                        PlayerScreen(
                            playerManager = playerManager,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                val title = uri.lastPathSegment ?: "External Video"
                playerManager.playMedia(uri, title)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Auto-enter PiP when user leaves app while video is actively playing
        val isPlaying = playerManager.getPlayer()?.isPlaying == true
        if (isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            playerManager.setControlsVisibility(false)
        } else {
            playerManager.setControlsVisibility(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}
