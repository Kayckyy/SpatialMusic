package com.openmind.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmind.domain.MusicFolder
import com.openmind.domain.Track
import com.openmind.ui.theme.Accent

// ── Navegação principal ────────────────────────────────────────────────────
sealed class Screen {
    object Folders   : Screen()
    data class Tracks(val folder: MusicFolder) : Screen()
    object Settings  : Screen()
}

@Composable
fun MainScreen(
    folders: List<MusicFolder>,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    progress: Float,
    shuffle: Boolean,
    repeat: Boolean,
    audioSettings: AudioSettings,
    onTrackClick: (Track) -> Unit,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSettingsChange: (AudioSettings) -> Unit,
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Folders) }
    var playerExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Conteúdo da tela atual ──
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                slideInHorizontally { it / 3 } + fadeIn() togetherWith
                slideOutHorizontally { -it / 3 } + fadeOut()
            },
            modifier = Modifier.fillMaxSize(),
            label = "screen",
        ) { currentScreen ->
            when (currentScreen) {
                is Screen.Folders -> FolderListScreen(
                    folders        = folders,
                    onFolderClick  = { screen = Screen.Tracks(it) },
                    onSettingsClick = { screen = Screen.Settings },
                )
                is Screen.Tracks -> TrackListScreen(
                    folder        = currentScreen.folder,
                    tracks        = tracks,
                    currentTrack  = currentTrack,
                    isPlaying     = isPlaying,
                    onTrackClick  = onTrackClick,
                    onBackClick   = { screen = Screen.Folders },
                )
                is Screen.Settings -> SettingsScreen(
                    settings         = audioSettings,
                    onSettingsChange = onSettingsChange,
                    onBackClick      = { screen = Screen.Folders },
                )
            }
        }

        // ── Player (mini ou full screen) ──
        if (currentTrack != null) {
            AnimatedContent(
                targetState = playerExpanded,
                transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith
                    slideOutVertically { it } + fadeOut()
                },
                modifier = if (playerExpanded)
                    Modifier.fillMaxSize()
                else
                    Modifier.align(Alignment.BottomCenter),
                label = "player",
            ) { expanded ->
                if (expanded) {
                    FullScreenPlayer(
                        track       = currentTrack,
                        isPlaying   = isPlaying,
                        progress    = progress,
                        shuffle     = shuffle,
                        repeat      = repeat,
                        accent      = Accent,
                        onPlayPause = onPlayPause,
                        onPrev      = onPrev,
                        onNext      = onNext,
                        onSeek      = onSeek,
                        onShuffle   = onShuffle,
                        onRepeat    = onRepeat,
                        onCollapse  = { playerExpanded = false },
                    )
                } else {
                    MiniPlayer(
                        track       = currentTrack,
                        isPlaying   = isPlaying,
                        progress    = progress,
                        accent      = Accent,
                        onPlayPause = onPlayPause,
                        onExpand    = { playerExpanded = true },
                    )
                }
            }
        }
    }
}
