package com.openmind.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.openmind.domain.Track
import com.openmind.ui.components.*
import com.openmind.ui.theme.*

// ── Mini Player ────────────────────────────────────────────────────────────
@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    progress: Float,
    accent: Color,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Surface)
            .clickable(onClick = onExpand),
    ) {
        // barra de progresso no topo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(SurfaceBorder),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(accent),
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrackArtwork(color = SurfaceHigh, size = 46.dp)

            Column(Modifier.weight(1f)) {
                Text(
                    text     = track.title,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = OnSurface,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
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

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = OnSurface,
                    modifier = Modifier.size(28.dp),
                )
            }

            IconButton(onClick = { /* next */ }) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = OnSurfaceDim)
            }
        }
    }
}

// ── Full Screen Player ─────────────────────────────────────────────────────
@Composable
fun FullScreenPlayer(
    track: Track,
    isPlaying: Boolean,
    progress: Float,
    shuffle: Boolean,
    repeat: Boolean,
    accent: Color,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onCollapse: () -> Unit,
) {
    // artwork rotation
    val rotation = rememberInfiniteTransition(label = "artwork")
    val artworkAngle by rotation.animateFloat(
        initialValue   = 0f,
        targetValue    = 360f,
        animationSpec  = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label          = "rotate",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Handle
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SurfaceBorder)
                .clickable(onClick = onCollapse),
        )
        Spacer(Modifier.height(8.dp))

        // Botão fechar
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onCollapse) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Recolher", tint = OnSurfaceDim, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Artwork
        Box(
            modifier = Modifier
                .size(256.dp)
                .clip(CircleShape)
                .rotate(if (isPlaying) artworkAngle else artworkAngle)
                .shadow(elevation = 32.dp, shape = CircleShape),
        ) {
            TrackArtwork(
                color = SurfaceHigh,
                size  = 256.dp,
                shape = RoundedCornerShape(128.dp),
            )
            // anel interno
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Background),
            )
        }

        Spacer(Modifier.height(36.dp))

        // Título + artista
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text  = track.title,
                style = MaterialTheme.typography.displaySmall,
                color = OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${track.artist} · ${track.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(28.dp))

        // Slider
        ProgressSlider(
            progress = progress,
            onSeek   = onSeek,
            accent   = accent,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(track.progressFormatted(progress), style = MaterialTheme.typography.bodySmall, color = OnSurfaceFaint)
            Text(track.durationFormatted, style = MaterialTheme.typography.bodySmall, color = OnSurfaceFaint)
        }

        Spacer(Modifier.height(24.dp))

        // Controles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = if (shuffle) accent else OnSurfaceFaint,
                )
            }

            ControlButton(onClick = onPrev, size = 52.dp) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = OnSurface, modifier = Modifier.size(26.dp))
            }

            ControlButton(onClick = onPlayPause, size = 70.dp, filled = true, accent = accent) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Background,
                    modifier = Modifier.size(34.dp),
                )
            }

            ControlButton(onClick = onNext, size = 52.dp) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = OnSurface, modifier = Modifier.size(26.dp))
            }

            IconButton(onClick = onRepeat) {
                Icon(
                    Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = if (repeat) accent else OnSurfaceFaint,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SubtleDivider()
        Spacer(Modifier.height(12.dp))

        // Chip de azimute HRTF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.SpatialAudio, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text("Binaural 3D", style = MaterialTheme.typography.bodySmall, color = accent)
            Spacer(Modifier.weight(1f))
            Text("az 90°", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
        }
    }
}
