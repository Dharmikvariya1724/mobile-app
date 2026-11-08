package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import com.example.data.model.MediaEntity
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.settings.AboutPrivacyScreen
import com.example.ui.screens.settings.ChangePasswordScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.StorageInfoScreen
import com.example.ui.screens.viewer.PhotoViewerScreen
import com.example.ui.screens.viewer.VideoPlayerScreen
import com.example.ui.theme.PrivateVaultTheme
import com.example.ui.theme.VaultBackground
import com.example.ui.viewmodel.VaultViewModel
import com.example.ui.viewmodel.VaultViewModelFactory

sealed class VaultScreen {
    data object Dashboard : VaultScreen()
    data class PhotoViewer(val media: MediaEntity) : VaultScreen()
    data class VideoPlayer(val media: MediaEntity) : VaultScreen()
    data object Settings : VaultScreen()
    data object ChangePassword : VaultScreen()
    data object StorageInfo : VaultScreen()
    data object AboutPrivacy : VaultScreen()
}

@androidx.compose.material3.ExperimentalMaterial3Api
class MainActivity : FragmentActivity() {

    private val viewModel: VaultViewModel by viewModels {
        val app = application as VaultApplication
        VaultViewModelFactory(app.vaultRepository, app.mediaRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            PrivateVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VaultBackground
                ) {
                    val isUnlocked by viewModel.isUnlocked.collectAsState()
                    var currentScreen by remember { mutableStateOf<VaultScreen>(VaultScreen.Dashboard) }

                    if (!isUnlocked) {
                        AuthScreen(
                            viewModel = viewModel,
                            onAuthenticated = {
                                currentScreen = VaultScreen.Dashboard
                            }
                        )
                    } else {
                        // Navigation back handler
                        BackHandler(enabled = currentScreen != VaultScreen.Dashboard) {
                            currentScreen = when (currentScreen) {
                                is VaultScreen.ChangePassword,
                                is VaultScreen.StorageInfo,
                                is VaultScreen.AboutPrivacy -> VaultScreen.Settings
                                else -> VaultScreen.Dashboard
                            }
                        }

                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "vault_navigation"
                        ) { screen ->
                            when (screen) {
                                is VaultScreen.Dashboard -> {
                                    DashboardScreen(
                                        viewModel = viewModel,
                                        onOpenPhoto = { media ->
                                            currentScreen = VaultScreen.PhotoViewer(media)
                                        },
                                        onOpenVideo = { media ->
                                            currentScreen = VaultScreen.VideoPlayer(media)
                                        },
                                        onOpenSettings = {
                                            currentScreen = VaultScreen.Settings
                                        }
                                    )
                                }
                                is VaultScreen.PhotoViewer -> {
                                    PhotoViewerScreen(
                                        media = screen.media,
                                        viewModel = viewModel,
                                        onBack = { currentScreen = VaultScreen.Dashboard }
                                    )
                                }
                                is VaultScreen.VideoPlayer -> {
                                    VideoPlayerScreen(
                                        media = screen.media,
                                        viewModel = viewModel,
                                        onBack = { currentScreen = VaultScreen.Dashboard }
                                    )
                                }
                                is VaultScreen.Settings -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = VaultScreen.Dashboard },
                                        onNavigateChangePassword = { currentScreen = VaultScreen.ChangePassword },
                                        onNavigateStorageInfo = { currentScreen = VaultScreen.StorageInfo },
                                        onNavigateAboutPrivacy = { currentScreen = VaultScreen.AboutPrivacy }
                                    )
                                }
                                is VaultScreen.ChangePassword -> {
                                    ChangePasswordScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = VaultScreen.Settings }
                                    )
                                }
                                is VaultScreen.StorageInfo -> {
                                    StorageInfoScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = VaultScreen.Settings }
                                    )
                                }
                                is VaultScreen.AboutPrivacy -> {
                                    AboutPrivacyScreen(
                                        onBack = { currentScreen = VaultScreen.Settings }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
