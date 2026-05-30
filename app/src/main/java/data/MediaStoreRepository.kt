package com.openmind.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.openmind.domain.MusicFolder
import com.openmind.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreRepository(private val context: Context) {

    // ── Lê todas as faixas de áudio do MediaStore ──────────────────────────
    suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val uri    = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,          // path absoluto
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(uri, projection, selection, null, sortOrder)
            ?.use { cursor ->
                val idCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val pathCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val albumId    = cursor.getLong(albumIdCol)
                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId
                    )
                    tracks.add(Track(
                        id           = cursor.getLong(idCol),
                        title        = cursor.getString(titleCol) ?: "Sem título",
                        artist       = cursor.getString(artistCol) ?: "Artista desconhecido",
                        album        = cursor.getString(albumCol)  ?: "Álbum desconhecido",
                        path         = cursor.getString(pathCol),
                        durationMs   = cursor.getLong(durCol),
                        albumArtUri  = artworkUri,
                    ))
                }
            }
        tracks
    }

    // ── Agrupa por pasta ───────────────────────────────────────────────────
    suspend fun loadFolders(): List<MusicFolder> = withContext(Dispatchers.IO) {
        loadTracks()
            .groupBy { File(it.path).parent ?: "/" }
            .map { (path, tracks) ->
                MusicFolder(
                    id         = path.hashCode().toLong(),
                    name       = File(path).name,
                    path       = path,
                    trackCount = tracks.size,
                )
            }
            .sortedBy { it.name }
    }

    // ── Faixas de uma pasta específica ────────────────────────────────────
    suspend fun tracksInFolder(folder: MusicFolder): List<Track> =
        withContext(Dispatchers.IO) {
            if (folder.id == MusicFolder.ALL.id) loadTracks()
            else loadTracks().filter { File(it.path).parent == folder.path }
        }
}
