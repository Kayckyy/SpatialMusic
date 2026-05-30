package com.openmind.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Paleta base — escuro sólido ────────────────────────────────────────────
val Background    = Color(0xFF0E0E12)
val Surface       = Color(0xFF1A1A22)
val SurfaceHigh   = Color(0xFF242430)
val SurfaceBorder = Color(0xFF2E2E3A)
val OnSurface     = Color(0xFFE2E2F0)
val OnSurfaceDim  = Color(0xFF8888A0)
val OnSurfaceFaint= Color(0xFF44445A)

// Acento padrão — substituído dinamicamente pela cor da capa
val Accent        = Color(0xFF7C8FF5)
val AccentDim     = Color(0xFF3D4A8A)

private val DarkColorScheme = darkColorScheme(
    background        = Background,
    surface           = Surface,
    surfaceVariant    = SurfaceHigh,
    primary           = Accent,
    onPrimary         = Color(0xFF0E0E12),
    onBackground      = OnSurface,
    onSurface         = OnSurface,
    onSurfaceVariant  = OnSurfaceDim,
    outline           = SurfaceBorder,
)

@Composable
fun OpenMindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = OpenMindTypography,
        content     = content,
    )
}
