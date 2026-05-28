package com.solo.soloplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solo.soloplayer.ui.screen.formatTicksToTime
import com.solo.soloplayer.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@Composable
fun PlaybackController(
    viewModel: PlayerViewModel,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible by viewModel.isControllerVisible.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isIso by viewModel.isDvdIso.collectAsState()

    // 10-Second Auto-Fadeout Logic
    LaunchedEffect(isVisible, isPlaying, position) {
        if (isVisible) {
            delay(10000L) // Wait 10s of inactivity
            viewModel.setControllerVisible(false)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xDD09090D)),
                        startY = 100f
                    )
                )
                .onKeyEvent {
                    // Any remote control key event resets the idle fadeout timer
                    viewModel.setControllerVisible(true)
                    false
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Seek Bar (Focusable Slider)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = formatTicksToTime(position),
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    var isSliderFocused by remember { mutableStateOf(false) }
                    Slider(
                        value = if (duration > 0) position.toFloat() else 0f,
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        onValueChange = { onSeek(it.toLong()) },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isSliderFocused = it.isFocused }
                            .focusable(),
                        colors = SliderDefaults.colors(
                            thumbColor = if (isSliderFocused) Color(0xFFFF2E93) else Color.White,
                            activeTrackColor = Color(0xFFFF2E93),
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )

                    Text(
                        text = formatTicksToTime(duration),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                // Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip Backward
                    var isRewFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { onSeek(kotlin.math.max(0L, position - 100_000_000L)) }, // 10s backward
                        modifier = Modifier
                            .onFocusChanged { isRewFocused = it.isFocused }
                            .size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = if (isRewFocused) Color(0xFFFF2E93) else Color(0x22FFFFFF)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind",
                                tint = Color.White
                            )
                        }
                    }

                    // Play/Pause
                    var isPlayFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .onFocusChanged { isPlayFocused = it.isFocused }
                            .size(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = if (isPlayFocused) Color(0xFFFF2E93) else Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = if (isPlayFocused) Color.White else Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Skip Forward
                    var isFwdFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { onSeek(kotlin.math.min(duration, position + 100_000_000L)) }, // 10s forward
                        modifier = Modifier
                            .onFocusChanged { isFwdFocused = it.isFocused }
                            .size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = if (isFwdFocused) Color(0xFFFF2E93) else Color(0x22FFFFFF)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Fast Forward",
                                tint = Color.White
                            )
                        }
                    }

                    // Disc Menu (For ISO only)
                    if (isIso) {
                        var isMenuFocused by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { viewModel.toggleDiscMenu(true) },
                            modifier = Modifier
                                .onFocusChanged { isMenuFocused = it.isFocused }
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = if (isMenuFocused) Color(0xFFFF2E93) else Color(0x22FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Disc Menu",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Disc Menu",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom modifier helper to avoid unresolved size issues
private fun Modifier.size(size: androidx.compose.ui.unit.Dp): Modifier = this.width(size).height(size)
