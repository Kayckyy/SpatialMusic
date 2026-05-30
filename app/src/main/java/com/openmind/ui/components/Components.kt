package com.openmind.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.openmind.ui.theme.*

// ── Artwork placeholder ────────────────────────────────────────────────────
// Substituir pelo AsyncImage (Coil) quando integrar MediaStore
@Composable
fun TrackArtwork(
    color: Color = SurfaceHigh,
    size: Dp = 52.dp,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            // placeholder — trocar por AsyncImage com bitmap da capa
            imageVector = androidx.compose.material.icons.Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = OnSurfaceFaint,
            modifier = Modifier.size(size * 0.4f),
        )
    }
}

// ── Waveform animado ───────────────────────────────────────────────────────
@Composable
fun WaveformBars(playing: Boolean, color: Color, modifier: Modifier = Modifier) {
    val bars = 5
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(bars) { i ->
            val infiniteTransition = rememberInfiniteTransition(label = "wave$i")
            val height by infiniteTransition.animateFloat(
                initialValue = 3f,
                targetValue  = if (playing) (8f + i * 3f) else 3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300 + i * 80, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar$i",
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(if (playing) height.dp else 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

// ── Chip de label (ex: número de faixas) ──────────────────────────────────
@Composable
fun CountChip(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceHigh)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceDim,
        )
    }
}

// ── Slider de progresso ────────────────────────────────────────────────────
@Composable
fun ProgressSlider(
    progress: Float,
    onSeek: (Float) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Slider(
        value    = progress,
        onValueChange = onSeek,
        modifier = modifier,
        colors   = SliderDefaults.colors(
            thumbColor            = Color.White,
            activeTrackColor      = accent,
            inactiveTrackColor    = SurfaceBorder,
        ),
        thumb = {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        },
    )
}

// ── Botão de controle circular ─────────────────────────────────────────────
@Composable
fun ControlButton(
    onClick: () -> Unit,
    size: Dp = 48.dp,
    filled: Boolean = false,
    accent: Color = Accent,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) accent else SurfaceHigh)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ── Divisor sutil ──────────────────────────────────────────────────────────
@Composable
fun SubtleDivider() {
    HorizontalDivider(color = SurfaceBorder, thickness = 0.5.dp)
}
