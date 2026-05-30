package com.openmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openmind.domain.MusicFolder
import com.openmind.domain.Track
import com.openmind.ui.components.*
import com.openmind.ui.theme.*

// ── Tela de pastas ─────────────────────────────────────────────────────────
@Composable
fun FolderListScreen(
    folders: List<MusicFolder>,
    onFolderClick: (MusicFolder) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "BIBLIOTECA",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceDim,
                )
                Text(
                    text = "OpenMind",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Rounded.Settings, contentDescription = "Configurações", tint = OnSurfaceDim)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Card "Todas as músicas"
            item {
                FolderCard(
                    name     = "Todas as músicas",
                    subtitle = "Biblioteca completa",
                    count    = folders.sumOf { it.trackCount },
                    accent   = Accent,
                    onClick  = { onFolderClick(MusicFolder.ALL) },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "PASTAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceFaint,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }

            itemsIndexed(folders) { _, folder ->
                FolderCard(
                    name     = folder.name,
                    subtitle = folder.path,
                    count    = folder.trackCount,
                    onClick  = { onFolderClick(folder) },
                )
            }
        }
    }
}

@Composable
private fun FolderCard(
    name: String,
    subtitle: String,
    count: Int,
    accent: Color = SurfaceHigh,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Folder, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CountChip(count)
    }
}

// ── Tela de faixas ─────────────────────────────────────────────────────────
@Composable
fun TrackListScreen(
    folder: MusicFolder,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar", tint = OnSurface)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = folder.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceDim,
                )
                Text(
                    text = "${tracks.size} faixas",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                )
            }
        }

        SubtleDivider()

        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            itemsIndexed(tracks) { index, track ->
                val isActive = track.id == currentTrack?.id
                TrackRow(
                    index    = index + 1,
                    track    = track,
                    isActive = isActive,
                    isPlaying = isPlaying && isActive,
                    onClick  = { onTrackClick(track) },
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    index: Int,
    track: Track,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) SurfaceHigh else Background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // índice ou waveform
        Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            if (isActive) {
                WaveformBars(playing = isPlaying, color = Accent, modifier = Modifier.height(16.dp))
            } else {
                Text(
                    text  = "$index",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceFaint,
                )
            }
        }

        TrackArtwork(size = 46.dp)

        Column(Modifier.weight(1f)) {
            Text(
                text  = track.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) Accent else OnSurface,
                fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.SemiBold else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text  = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim,
                maxLines = 1,
            )
        }

        Text(
            text  = track.durationFormatted,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceFaint,
        )
    }
}
