package com.openmind.domain

import android.net.Uri
import java.io.File

// ── MusicFolder ────────────────────────────────────────────────────────────
data class MusicFolder(
    val id: Long,
    val name: String,
    val path: String,
    val trackCount: Int,
) {
    companion object {
        // Sentinela para "Todas as músicas"
        val ALL = MusicFolder(id = -1L, name = "Todas as músicas", path = "", trackCount = 0)
    }
}
