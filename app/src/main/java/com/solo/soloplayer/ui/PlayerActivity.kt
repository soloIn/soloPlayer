package com.solo.soloplayer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.solo.soloplayer.ui.screen.PlayerScreen
import com.solo.soloplayer.ui.viewmodel.PlayerViewModel
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import androidx.media3.common.Player as ExoPlayerCommon
import androidx.media3.exoplayer.ExoPlayer

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    // ExoPlayer
    private var exoPlayer: ExoPlayer? = null

    // libVLC
    private var libVlc: LibVLC? = null
    private var libVlcMediaPlayer: MediaPlayer? = null
    private var vlcVideoLayout: VLCVideoLayout? = null

    private var updateJob: Job? = null
    private var isIsoFile = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val movieId = intent.getStringExtra(EXTRA_MOVIE_ID) ?: return
        val playWithDiscMenu = intent.getBooleanExtra(EXTRA_PLAY_WITH_DISC_MENU, false)

        viewModel.setMovieId(movieId)

        // Observe movie detail selection to set up player engine
        lifecycleScope.launch {
            viewModel.currentMovie.collectLatest { movie ->
                if (movie != null) {
                    isIsoFile = movie.videoType.uppercase() == "ISO" || movie.rawFilePath.lowercase().endsWith(".iso")
                    setupPlayer(movie.rawFilePath)
                    if (playWithDiscMenu && isIsoFile) {
                        viewModel.toggleDiscMenu(true)
                    }
                }
            }
        }

        setContent {
            PlayerScreen(
                viewModel = viewModel,
                exoPlayer = exoPlayer,
                vlcVideoLayout = vlcVideoLayout,
                onSeek = { ticks -> seekTo(ticks) },
                onTogglePlay = { togglePlay() },
                onSelectAudioIndex = { selectAudioTrack(it) },
                onSelectSubtitleIndex = { selectSubtitleTrack(it) }
            )
        }
    }

    private fun setupPlayer(url: String) {
        // Release previous players if any
        releasePlayer()

        if (isIsoFile) {
            setupLibVlc(url)
        } else {
            setupExoPlayer(url)
        }

        startPlaybackUpdates()
    }

    private fun setupExoPlayer(url: String) {
        val player = ExoPlayer.Builder(this).build().apply {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(Uri.parse(url.ifBlank { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" }))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
        exoPlayer = player
        viewModel.setPlaying(true)
    }

    private fun setupLibVlc(url: String) {
        val args = ArrayList<String>()
        args.add("-vvv") // verbose logging
        args.add("--no-sub-autodetect-file")
        
        val instance = LibVLC(this, args)
        libVlc = instance

        val mp = MediaPlayer(instance)
        libVlcMediaPlayer = mp

        val layout = VLCVideoLayout(this)
        vlcVideoLayout = layout

        mp.attachViews(layout, null, true, false)

        val media = Media(instance, Uri.parse(url.ifBlank { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" }))
        media.setHWDecoderEnabled(true, true)
        mp.media = media
        media.release()

        mp.play()
        viewModel.setPlaying(true)
    }

    private fun seekTo(ticks: Long) {
        val ms = ticks / 10_000
        if (isIsoFile) {
            libVlcMediaPlayer?.time = ms
        } else {
            exoPlayer?.seekTo(ms)
        }
        viewModel.setPlaybackPosition(ticks)
    }

    private fun togglePlay() {
        if (isIsoFile) {
            val mp = libVlcMediaPlayer ?: return
            if (mp.isPlaying) {
                mp.pause()
                viewModel.setPlaying(false)
            } else {
                mp.play()
                viewModel.setPlaying(true)
            }
        } else {
            val ep = exoPlayer ?: return
            if (ep.isPlaying) {
                ep.pause()
                viewModel.setPlaying(false)
            } else {
                ep.play()
                viewModel.setPlaying(true)
            }
        }
    }

    private fun selectAudioTrack(index: Int) {
        // libVLC specific audio track toggle
        if (isIsoFile) {
            libVlcMediaPlayer?.audioTrack = index
        }
    }

    private fun selectSubtitleTrack(index: Int) {
        // libVLC specific subtitle track toggle
        if (isIsoFile) {
            libVlcMediaPlayer?.spuTrack = index
        }
    }

    private fun startPlaybackUpdates() {
        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            while (true) {
                if (isIsoFile) {
                    libVlcMediaPlayer?.let { mp ->
                        val durationMs = mp.length
                        val positionMs = mp.time
                        viewModel.setDuration(durationMs * 10_000)
                        viewModel.setPlaybackPosition(positionMs * 10_000)
                        viewModel.setPlaying(mp.isPlaying)
                    }
                } else {
                    exoPlayer?.let { ep ->
                        val durationMs = ep.duration
                        val positionMs = ep.currentPosition
                        if (durationMs > 0) {
                            viewModel.setDuration(durationMs * 10_000)
                        }
                        viewModel.setPlaybackPosition(positionMs * 10_000)
                        viewModel.setPlaying(ep.isPlaying)
                    }
                }
                delay(1000)
            }
        }
    }

    // INTERCEPT REMOTE D-PAD KEYS FOR DVD MENU NAVIGATION
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Check if DVD ISO is playing and custom disc menu overlay is NOT open
        if (isIsoFile && !viewModel.isDiscMenuOpen.value) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                val navigated = when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        libVlcMediaPlayer?.navigate(MediaPlayer.Navigate.Up)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        libVlcMediaPlayer?.navigate(MediaPlayer.Navigate.Down)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        libVlcMediaPlayer?.navigate(MediaPlayer.Navigate.Left)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        libVlcMediaPlayer?.navigate(MediaPlayer.Navigate.Right)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        libVlcMediaPlayer?.navigate(MediaPlayer.Navigate.Activate)
                        true
                    }
                    else -> false
                }
                if (navigated) {
                    // Reset auto-hide controller timer on navigation action
                    viewModel.setControllerVisible(true)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun releasePlayer() {
        updateJob?.cancel()
        
        exoPlayer?.release()
        exoPlayer = null

        libVlcMediaPlayer?.let {
            it.stop()
            it.detachViews()
            it.release()
        }
        libVlcMediaPlayer = null

        libVlc?.release()
        libVlc = null
        vlcVideoLayout = null
    }

    override fun onPause() {
        super.onPause()
        if (isIsoFile) {
            libVlcMediaPlayer?.pause()
        } else {
            exoPlayer?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    companion object {
        private const val EXTRA_MOVIE_ID = "movie_id"
        private const val EXTRA_PLAY_WITH_DISC_MENU = "play_with_disc_menu"

        fun createIntent(context: Context, movieId: String, playWithDiscMenu: Boolean): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_MOVIE_ID, movieId)
                putExtra(EXTRA_PLAY_WITH_DISC_MENU, playWithDiscMenu)
            }
        }
    }
}
