package com.openmind.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openmind.data.MediaStoreRepository
import com.openmind.domain.MusicFolder
import com.openmind.domain.Track
import com.openmind.engine.HrtfEngine
import com.openmind.ui.screens.AudioSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlayerState(
    val folders: List<MusicFolder>     = emptyList(),
    val tracks: List<Track>            = emptyList(),
    val currentTrack: Track?           = null,
    val isPlaying: Boolean             = false,
    val progress: Float                = 0f,
    val shuffle: Boolean               = false,
    val repeat: Boolean                = false,
    val audioSettings: AudioSettings   = AudioSettings(),
    val loading: Boolean               = true,
)

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo   = MediaStoreRepository(app)
    private val engine = HrtfEngine()  // JNI bridge

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    init {
        loadLibrary()
        loadMedia()
        tickProgress()
    }

    private fun loadLibrary() {
        System.loadLibrary("openmind")
        engine.init(
            hrtfDir  = app.assets.openFd("hrtf").fileDescriptor.toString(),
            azimuth  = _state.value.audioSettings.azimuth,
            elevation = _state.value.audioSettings.elevation,
            crossfeed = _state.value.audioSettings.crossfeed,
            inputGain = _state.value.audioSettings.inputGain,
        )
    }

    private fun loadMedia() = viewModelScope.launch {
        val folders = repo.loadFolders()
        val tracks  = repo.loadTracks()
        _state.update { it.copy(folders = folders, tracks = tracks, loading = false) }
    }

    // ── Controles ──────────────────────────────────────────────────────────

    fun play(track: Track) {
        _state.update { it.copy(currentTrack = track, isPlaying = true, progress = 0f) }
        engine.play(track.path)
    }

    fun togglePlayPause() {
        val playing = !_state.value.isPlaying
        _state.update { it.copy(isPlaying = playing) }
        if (playing) engine.resume() else engine.pause()
    }

    fun next() {
        val s = _state.value
        val tracks = s.tracks
        if (tracks.isEmpty()) return
        val idx = tracks.indexOfFirst { it.id == s.currentTrack?.id }
        val next = if (s.shuffle) tracks.random()
                   else tracks.getOrNull(idx + 1) ?: tracks.first()
        play(next)
    }

    fun prev() {
        val s = _state.value
        if (s.progress > 0.05f) { seek(0f); return }
        val tracks = s.tracks
        val idx = tracks.indexOfFirst { it.id == s.currentTrack?.id }
        val prev = tracks.getOrNull(idx - 1) ?: tracks.last()
        play(prev)
    }

    fun seek(progress: Float) {
        _state.update { it.copy(progress = progress) }
        engine.seek(progress)
    }

    fun toggleShuffle() = _state.update { it.copy(shuffle = !it.shuffle) }
    fun toggleRepeat()  = _state.update { it.copy(repeat  = !it.repeat) }

    fun updateSettings(settings: AudioSettings) {
        _state.update { it.copy(audioSettings = settings) }
        engine.setAzimuth(settings.azimuth)
        engine.setCrossfeed(settings.crossfeed)
        engine.setInputGain(settings.inputGain)
        engine.setBlockSize(settings.blockSize)
    }

    fun tracksInFolder(folder: MusicFolder): List<Track> {
        if (folder.id == MusicFolder.ALL.id) return _state.value.tracks
        return _state.value.tracks.filter {
            it.path.startsWith(folder.path)
        }
    }

    // ── Tick de progresso ──────────────────────────────────────────────────
    private fun tickProgress() = viewModelScope.launch {
        while (true) {
            delay(200)
            val s = _state.value
            if (s.isPlaying && s.currentTrack != null) {
                val newProgress = engine.getProgress()
                if (newProgress >= 1f) {
                    if (s.repeat) play(s.currentTrack)
                    else next()
                } else {
                    _state.update { it.copy(progress = newProgress) }
                }
            }
        }
    }

    override fun onCleared() {
        engine.release()
        super.onCleared()
    }
}
