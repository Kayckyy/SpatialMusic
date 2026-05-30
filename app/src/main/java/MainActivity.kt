package com.openmind

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.openmind.ui.screens.MainScreen
import com.openmind.ui.theme.OpenMindTheme
import com.openmind.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val vm: PlayerViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissões concedidas — MediaStore carrega automaticamente */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // tela cheia — deixa o Compose controlar as insets
        WindowCompat.setDecorFitsSystemWindows(window, false)

        requestPermissions()

        setContent {
            OpenMindTheme {
                val state by vm.state.collectAsState()
                var currentFolderTracks by remember { mutableStateOf(state.tracks) }

                MainScreen(
                    folders        = state.folders,
                    tracks         = currentFolderTracks,
                    currentTrack   = state.currentTrack,
                    isPlaying      = state.isPlaying,
                    progress       = state.progress,
                    shuffle        = state.shuffle,
                    repeat         = state.repeat,
                    audioSettings  = state.audioSettings,
                    onTrackClick   = { vm.play(it) },
                    onPlayPause    = { vm.togglePlayPause() },
                    onPrev         = { vm.prev() },
                    onNext         = { vm.next() },
                    onSeek         = { vm.seek(it) },
                    onShuffle      = { vm.toggleShuffle() },
                    onRepeat       = { vm.toggleRepeat() },
                    onSettingsChange = { vm.updateSettings(it) },
                    onFolderOpen   = { folder ->
                        currentFolderTracks = vm.tracksInFolder(folder)
                    },
                )
            }
        }
    }

    private fun requestPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(perms)
    }
}
