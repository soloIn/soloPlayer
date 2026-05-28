package com.solo.soloplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solo.soloplayer.data.local.dao.ChapterDao
import com.solo.soloplayer.data.local.dao.MediaItemDao
import com.solo.soloplayer.data.local.dao.MovieDao
import com.solo.soloplayer.data.local.entity.ChapterEntity
import com.solo.soloplayer.data.local.entity.MediaItemEntity
import com.solo.soloplayer.data.local.entity.MovieEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val movieDao: MovieDao,
    private val chapterDao: ChapterDao,
    private val mediaItemDao: MediaItemDao
) : ViewModel() {

    private val _currentMovieId = MutableStateFlow<String?>(null)
    val currentMovieId: StateFlow<String?> = _currentMovieId.asStateFlow()

    // Current playing movie details
    val currentMovie: StateFlow<MovieEntity?> = _currentMovieId.flatMapLatest { id ->
        if (id == null) flowOf(null) else {
            flowOf(movieDao.getMovieById(id))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current playing chapters
    val chapters: StateFlow<List<ChapterEntity>> = _currentMovieId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else {
            chapterDao.getChaptersForMovie(id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player controls state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Tracks details
    private val _audioTracks = MutableStateFlow<List<String>>(emptyList())
    val audioTracks: StateFlow<List<String>> = _audioTracks.asStateFlow()

    private val _selectedAudioTrack = MutableStateFlow(-1)
    val selectedAudioTrack: StateFlow<Int> = _selectedAudioTrack.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<String>>(emptyList())
    val subtitleTracks: StateFlow<List<String>> = _subtitleTracks.asStateFlow()

    private val _selectedSubtitleTrack = MutableStateFlow(-1)
    val selectedSubtitleTrack: StateFlow<Int> = _selectedSubtitleTrack.asStateFlow()

    // Overlay Flags
    private val _isControllerVisible = MutableStateFlow(true)
    val isControllerVisible: StateFlow<Boolean> = _isControllerVisible.asStateFlow()

    private val _isDiscMenuOpen = MutableStateFlow(false)
    val isDiscMenuOpen: StateFlow<Boolean> = _isDiscMenuOpen.asStateFlow()

    // DVD/Blu-ray specific interactions
    private val _isDvdIso = MutableStateFlow(false)
    val isDvdIso: StateFlow<Boolean> = _isDvdIso.asStateFlow()

    // Track user key events for VLC DVD ISO menu navigation
    private val _dvdNavCommand = MutableSharedFlow<DvdNavKey>()
    val dvdNavCommand: SharedFlow<DvdNavKey> = _dvdNavCommand.asSharedFlow()

    enum class DvdNavKey {
        UP, DOWN, LEFT, RIGHT, SELECT
    }

    fun setMovieId(id: String) {
        _currentMovieId.value = id
        // Check if ISO file
        viewModelScope.launch {
            val movie = movieDao.getMovieById(id)
            if (movie != null) {
                val isIso = movie.videoType.uppercase() == "ISO" || movie.rawFilePath.lowercase().endsWith(".iso")
                _isDvdIso.value = isIso
            }
        }
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun setPlaybackPosition(position: Long) {
        _playbackPosition.value = position
        // Periodically update media_items progress in the DB
        val movieId = _currentMovieId.value
        if (movieId != null) {
            viewModelScope.launch {
                val movie = movieDao.getMovieById(movieId)
                if (movie != null) {
                    val mediaItem = MediaItemEntity(
                        id = movie.id,
                        title = movie.title,
                        url = movie.rawFilePath,
                        mediaType = movie.videoType,
                        duration = _duration.value,
                        lastPlaybackPosition = position,
                        lastPlayedTime = System.currentTimeMillis()
                    )
                    mediaItemDao.insertMediaItem(mediaItem)
                }
            }
        }
    }

    fun setDuration(dur: Long) {
        _duration.value = dur
    }

    fun setControllerVisible(visible: Boolean) {
        _isControllerVisible.value = visible
    }

    fun toggleDiscMenu(open: Boolean) {
        _isDiscMenuOpen.value = open
        if (open) {
            // Hide controller if disc menu opens
            _isControllerVisible.value = false
        }
    }

    fun setAudioTracks(tracks: List<String>) {
        _audioTracks.value = tracks
    }

    fun selectAudioTrack(index: Int) {
        _selectedAudioTrack.value = index
    }

    fun setSubtitleTracks(tracks: List<String>) {
        _subtitleTracks.value = tracks
    }

    fun selectSubtitleTrack(index: Int) {
        _selectedSubtitleTrack.value = index
    }

    fun sendDvdNavigation(key: DvdNavKey) {
        viewModelScope.launch {
            _dvdNavCommand.emit(key)
        }
    }
}
