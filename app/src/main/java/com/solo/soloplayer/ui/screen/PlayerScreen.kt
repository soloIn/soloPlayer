package com.solo.soloplayer.ui.screen

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.solo.soloplayer.ui.components.DiscMenuOverlay
import com.solo.soloplayer.ui.components.PlaybackController
import com.solo.soloplayer.ui.viewmodel.PlayerViewModel
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    exoPlayer: androidx.media3.common.Player?,
    vlcVideoLayout: VLCVideoLayout?,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onSelectAudioIndex: (Int) -> Unit,
    onSelectSubtitleIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isIso by viewModel.isDvdIso.collectAsState()
    val isDiscMenuOpen by viewModel.isDiscMenuOpen.collectAsState()
    val isControllerVisible by viewModel.isControllerVisible.collectAsState()

    val screenFocusRequester = remember { FocusRequester() }

    // Request initial focus on key wrapper to catch D-Pad inputs
    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(screenFocusRequester)
            .focusable()
            .onKeyEvent {
                // Show playback controls on any D-Pad action or key click
                if (!isDiscMenuOpen && !isControllerVisible) {
                    viewModel.setControllerVisible(true)
                }
                false
            }
            .clickable {
                if (!isDiscMenuOpen) {
                    viewModel.setControllerVisible(!isControllerVisible)
                }
            }
    ) {
        // 1. Video Surface Layer
        if (isIso) {
            // libVLC ISO surface
            if (vlcVideoLayout != null) {
                AndroidView(
                    factory = {
                        // Remove from parent if it was already attached
                        (vlcVideoLayout.parent as? ViewGroup)?.removeView(vlcVideoLayout)
                        vlcVideoLayout
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // ExoPlayer Standard surface
            if (exoPlayer != null) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false // Hide default ExoPlayer controller
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Playback Controller Overlay (Disabled/Unfocusable when Disc Menu is open)
        if (!isDiscMenuOpen) {
            PlaybackController(
                viewModel = viewModel,
                onSeek = onSeek,
                onTogglePlay = onTogglePlay,
                onPrevChapter = {},
                onNextChapter = {},
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Simplified DVD/Blu-ray Disc Menu Overlay
        DiscMenuOverlay(
            viewModel = viewModel,
            onSeek = onSeek,
            onPlayMovie = onTogglePlay,
            onSelectAudioIndex = onSelectAudioIndex,
            onSelectSubtitleIndex = onSelectSubtitleIndex,
            modifier = Modifier.fillMaxSize()
        )
    }
}
