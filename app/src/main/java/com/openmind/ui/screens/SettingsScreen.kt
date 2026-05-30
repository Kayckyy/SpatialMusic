package com.openmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import com.openmind.ui.components.SubtleDivider
import com.openmind.ui.theme.*

data class AudioSettings(
    val azimuth: Float       = 90f,    // 0=frente 90=esquerda 270=direita
    val elevation: Float     = 0f,
    val crossfeed: Float     = 0.08f,  // 0.0–0.30
    val inputGain: Float     = 0.35f,  // 0.1–1.0
    val blockSize: Int       = 2048,   // 512/1024/2048/4096
    val spatialEnabled: Boolean = true,
)

@Composable
fun SettingsScreen(
    settings: AudioSettings,
    onSettingsChange: (AudioSettings) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar", tint = OnSurface)
            }
            Text("Configurações", style = MaterialTheme.typography.titleLarge, color = OnSurface)
        }

        SubtleDivider()

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── HRTF / Espacial ──────────────────────────────────────────
            item {
                SectionHeader(icon = Icons.Rounded.SpatialAudio, title = "Áudio Espacial")
            }
            item {
                SettingsCard {
                    // Toggle principal
                    ToggleRow(
                        title    = "Processamento binaural",
                        subtitle = "True-Stereo HRTF via SADIE II",
                        checked  = settings.spatialEnabled,
                        onToggle = { onSettingsChange(settings.copy(spatialEnabled = it)) },
                    )

                    if (settings.spatialEnabled) {
                        SubtleDivider()
                        // Azimute
                        SliderRow(
                            title    = "Azimute",
                            subtitle = "${settings.azimuth.toInt()}° — ${azimuthLabel(settings.azimuth)}",
                            value    = settings.azimuth / 360f,
                            onSeek   = { onSettingsChange(settings.copy(azimuth = it * 360f)) },
                            accent   = Accent,
                        )
                        SubtleDivider()
                        // Elevação
                        SliderRow(
                            title    = "Elevação",
                            subtitle = "${settings.elevation.toInt()}°",
                            value    = (settings.elevation + 90f) / 180f,
                            onSeek   = { onSettingsChange(settings.copy(elevation = it * 180f - 90f)) },
                            accent   = Accent,
                        )
                        SubtleDivider()
                        // Crossfeed
                        SliderRow(
                            title    = "Crossfeed",
                            subtitle = "${"%.2f".format(settings.crossfeed)} — ${crossfeedLabel(settings.crossfeed)}",
                            value    = settings.crossfeed / 0.30f,
                            onSeek   = { onSettingsChange(settings.copy(crossfeed = it * 0.30f)) },
                            accent   = Accent,
                        )
                    }
                }
            }

            // ── Ganho e buffer ───────────────────────────────────────────
            item {
                SectionHeader(icon = Icons.Rounded.Tune, title = "Processamento")
            }
            item {
                SettingsCard {
                    SliderRow(
                        title    = "Ganho de entrada",
                        subtitle = "${"%.2f".format(settings.inputGain)} — compensa amplificação HRTF",
                        value    = settings.inputGain,
                        onSeek   = { onSettingsChange(settings.copy(inputGain = it)) },
                        accent   = Accent,
                    )
                    SubtleDivider()
                    // Block size — escolha discreta
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("Tamanho de bloco", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                        Text(
                            "${settings.blockSize} amostras · ${settings.blockSize * 1000 / 44100}ms latência",
                            style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(512, 1024, 2048, 4096).forEach { size ->
                                val selected = settings.blockSize == size
                                FilterChip(
                                    selected = selected,
                                    onClick  = { onSettingsChange(settings.copy(blockSize = size)) },
                                    label    = { Text("$size") },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentDim,
                                        selectedLabelColor     = Accent,
                                        containerColor         = SurfaceHigh,
                                        labelColor             = OnSurfaceDim,
                                    ),
                                    border = null,
                                )
                            }
                        }
                    }
                }
            }

            // ── Sobre ────────────────────────────────────────────────────
            item {
                SectionHeader(icon = Icons.Rounded.Info, title = "Sobre")
            }
            item {
                SettingsCard {
                    InfoRow("Engine", "True-Stereo HRTF · Overlap-Add FFT")
                    SubtleDivider()
                    InfoRow("Dataset", "SADIE II — Subject D1")
                    SubtleDivider()
                    InfoRow("Áudio", "AAudio · Oboe · Float32 PCM")
                    SubtleDivider()
                    InfoRow("Versão", "0.1.0-alpha")
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun azimuthLabel(az: Float) = when {
    az < 30 || az > 330 -> "Frente"
    az in 30f..150f     -> "Esquerda"
    az in 150f..210f    -> "Trás"
    else                -> "Direita"
}

private fun crossfeedLabel(cf: Float) = when {
    cf < 0.04f -> "Separação total"
    cf < 0.10f -> "Natural"
    cf < 0.18f -> "Centralizado"
    else       -> "Mono"
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Accent,
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface),
        content = content,
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Background,
                checkedTrackColor  = Accent,
                uncheckedTrackColor = SurfaceHigh,
            ),
        )
    }
}

@Composable
private fun SliderRow(
    title: String,
    subtitle: String,
    value: Float,
    onSeek: (Float) -> Unit,
    accent: Color,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
        Spacer(Modifier.height(6.dp))
        Slider(
            value = value,
            onValueChange = onSeek,
            colors = SliderDefaults.colors(
                thumbColor         = Color.White,
                activeTrackColor   = accent,
                inactiveTrackColor = SurfaceBorder,
            ),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}
