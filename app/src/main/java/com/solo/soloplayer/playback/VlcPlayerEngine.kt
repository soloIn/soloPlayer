package com.solo.soloplayer.playback

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import com.solo.soloplayer.domain.repository.SmbAccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.net.URI
import java.net.URLEncoder

class VlcPlayerEngine(
    private val context: Context,
    private val smbAccountRepository: SmbAccountRepository
) : PlaybackEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var videoSurfaceView: SurfaceView? = null
    private var videoTextureView: TextureView? = null
    private var currentMedia: PlayableMedia? = null

    override val currentPositionMs: Long
        get() = mediaPlayer?.time ?: 0L

    override val durationMs: Long
        get() = mediaPlayer?.length ?: 0L

    override fun initialize() {
        if (mediaPlayer != null) return

        val options = ArrayList<String>()
        options.add("--hardware-acceleration=1") // Enable hardware acceleration
        options.add("--codec=mediacodec_ndk")
        options.add("-vvv") // Verbose logging for debugging

        libVlc = LibVLC(context, options)
        mediaPlayer = MediaPlayer(libVlc).apply {
            setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Playing -> _playbackState.value = PlaybackState.Playing
                    MediaPlayer.Event.Paused -> _playbackState.value = PlaybackState.Paused
                    MediaPlayer.Event.Stopped -> _playbackState.value = PlaybackState.Idle
                    MediaPlayer.Event.EndReached -> _playbackState.value = PlaybackState.Ended
                    MediaPlayer.Event.EncounteredError -> _playbackState.value = PlaybackState.Error("libVLC player error")
                }
            }
        }

        // Attach video output if already set
        setVideoOutput(videoSurfaceView, videoTextureView)
    }

    override fun play(media: PlayableMedia) {
        currentMedia = media
        _playbackState.value = PlaybackState.Buffering

        scope.launch {
            val resolvedUrl = formatSmbPath(media.path)
            val vlc = libVlc ?: return@launch
            val player = mediaPlayer ?: return@launch

            val parsedUri = Uri.parse(resolvedUrl)
            val m = Media(vlc, parsedUri).apply {
                // Support ISO and standard file playback with hardware decoding (MediaCodec)
                setHWDecoderEnabled(true, true)
                addOption(":codec=mediacodec_ndk")
            }

            player.media = m
            m.release()

            val startPosMs = media.startPositionTicks / 10000L
            if (startPosMs > 0) {
                player.time = startPosMs
            }

            player.play()
        }
    }

    private suspend fun formatSmbPath(path: String): String = withContext(Dispatchers.IO) {
        if (!path.startsWith("smb://", ignoreCase = true)) return@withContext path

        try {
            val uri = URI(path)
            val host = uri.host ?: return@withContext path
            if (uri.userInfo != null) return@withContext path

            val accounts = smbAccountRepository.getAllAccounts().first()
            val account = accounts.firstOrNull { it.serverIp.equals(host, ignoreCase = true) }

            if (account != null) {
                val username = URLEncoder.encode(account.username, "UTF-8")
                val password = URLEncoder.encode(account.password, "UTF-8")
                val scheme = "smb://"
                val remainder = path.removePrefix(scheme)
                if (remainder.startsWith(host)) {
                    scheme + "$username:$password@" + remainder
                } else {
                    path
                }
            } else {
                path
            }
        } catch (e: Exception) {
            e.printStackTrace()
            path
        }
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun resume() {
        mediaPlayer?.play()
    }

    override fun seekTo(positionMs: Long) {
        mediaPlayer?.time = positionMs
    }

    override fun stop() {
        mediaPlayer?.stop()
        _playbackState.value = PlaybackState.Idle
    }

    override fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        libVlc?.release()
        libVlc = null
        _playbackState.value = PlaybackState.Idle
    }

    override fun setVideoOutput(surfaceView: SurfaceView?, textureView: TextureView?) {
        this.videoSurfaceView = surfaceView
        this.videoTextureView = textureView
        val player = mediaPlayer ?: return
        val vout = player.vlcVout
        if (surfaceView != null) {
            vout.setVideoView(surfaceView)
            vout.attachViews()
        } else if (textureView != null) {
            vout.setVideoView(textureView)
            vout.attachViews()
        } else {
            vout.detachViews()
        }
    }
}
