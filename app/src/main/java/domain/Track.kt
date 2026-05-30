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

// ── Track ──────────────────────────────────────────────────────────────────
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val durationMs: Long,
    val albumArtUri: Uri? = null,
) {
    val durationFormatted: String get() {
        val s = durationMs / 1000
        val m = s / 60
        val h = m / 60
        return if (h > 0) "%d:%02d:%02d".format(h, m % 60, s % 60)
        else "%d:%02d".format(m, s % 60)
    }

    fun progressFormatted(progress: Float): String {
        val ms = (durationMs * progress).toLong()
        val s  = ms / 1000
        val m  = s / 60
        val h  = m / 60
        return if (h > 0) "%d:%02d:%02d".format(h, m % 60, s % 60)
        else "%d:%02d".format(m, s % 60)
    }
}
