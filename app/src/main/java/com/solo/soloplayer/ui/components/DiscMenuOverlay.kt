package com.solo.soloplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.solo.soloplayer.data.local.entity.ChapterEntity
import com.solo.soloplayer.ui.screen.ChapterThumbnailCard
import com.solo.soloplayer.ui.viewmodel.PlayerViewModel

enum class DiscMenuOption(val title: String, val icon: ImageVector) {
    PLAY("Resume Film", Icons.Default.PlayArrow),
    SCENE_SELECTION("Scene Selection", Icons.Default.Movie),
    AUDIO_TRACKS("Audio Tracks", Icons.Default.Audiotrack),
    SUBTITLES("Subtitle Tracks", Icons.Default.Subtitles)
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun DiscMenuOverlay(
    viewModel: PlayerViewModel,
    onSeek: (Long) -> Unit,
    onPlayMovie: () -> Unit,
    onSelectAudioIndex: (Int) -> Unit,
    onSelectSubtitleIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOpen by viewModel.isDiscMenuOpen.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val audioTracks by viewModel.audioTracks.collectAsState()
    val selectedAudio by viewModel.selectedAudioTrack.collectAsState()
    val subtitleTracks by viewModel.subtitleTracks.collectAsState()
    val selectedSubtitle by viewModel.selectedSubtitleTrack.collectAsState()

    var activeOption by remember { mutableStateOf(DiscMenuOption.PLAY) }
    val initialFocusRequester = remember { FocusRequester() }

    // Request focus on menu entry for remote keys
    LaunchedEffect(isOpen) {
        if (isOpen) {
            initialFocusRequester.requestFocus()
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        // D-Pad Focus Trap: root Box consumes key events
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE09090D))
                .focusProperties {
                    onExit = { FocusRequester.Cancel }
                }
                .onKeyEvent { keyEvent ->
                    // Key interception: back key exits the menu
                    if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                        viewModel.toggleDiscMenu(false)
                        true
                    } else {
                        false
                    }
                }
                .focusable()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Left Column: Core disc menu options (Width: 35%)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.35f)
                        .background(Color(0xFF14141E), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Disc Menu",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    DiscMenuOption.values().forEachIndexed { index, option ->
                        var isItemFocused by remember { mutableStateOf(false) }
                        val isSelected = option == activeOption

                        Surface(
                            onClick = {
                                activeOption = option
                                if (option == DiscMenuOption.PLAY) {
                                    viewModel.toggleDiscMenu(false)
                                    onPlayMovie()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .onFocusChanged { isItemFocused = it.isFocused }
                                .then(if (index == 0) Modifier.focusRequester(initialFocusRequester) else Modifier),
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isItemFocused -> Color(0xFFFF2E93)
                                isSelected -> Color(0x22FFFFFF)
                                else -> Color.Transparent
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.title,
                                    tint = if (isItemFocused) Color.White else Color(0xCCFFFFFF)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = option.title,
                                    color = if (isItemFocused) Color.White else Color(0xCCFFFFFF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // Right Column: Details Pane (Chapters/Audio/Subtitles) based on selection
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.65f)
                        .background(Color(0xFF14141E), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    when (activeOption) {
                        DiscMenuOption.PLAY -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Resume playback of the current movie.",
                                    color = Color(0x88FFFFFF),
                                    fontSize = 15.sp
                                )
                            }
                        }

                        DiscMenuOption.SCENE_SELECTION -> {
                            SceneSelectionPanel(
                                chapters = chapters,
                                onSelect = { ticks ->
                                    onSeek(ticks)
                                    viewModel.toggleDiscMenu(false)
                                }
                            )
                        }

                        DiscMenuOption.AUDIO_TRACKS -> {
                            TrackSelectionPanel(
                                title = "Audio Tracks",
                                tracks = audioTracks.ifEmpty { listOf("Track 1: English (AC3)", "Track 2: Japanese (DTS)") },
                                selectedIndex = selectedAudio,
                                onSelect = { index ->
                                    viewModel.selectAudioTrack(index)
                                    onSelectAudioIndex(index)
                                }
                            )
                        }

                        DiscMenuOption.SUBTITLES -> {
                            TrackSelectionPanel(
                                title = "Subtitle Tracks",
                                tracks = subtitleTracks.ifEmpty { listOf("Off", "Track 1: English (SRT)", "Track 2: English SDH") },
                                selectedIndex = selectedSubtitle,
                                onSelect = { index ->
                                    viewModel.selectSubtitleTrack(index)
                                    onSelectSubtitleIndex(index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SceneSelectionPanel(
    chapters: List<ChapterEntity>,
    onSelect: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Select Scene",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(chapters) { chapter ->
                ChapterThumbnailCard(
                    chapter = chapter,
                    onClick = { onSelect(chapter.startPositionTicks) }
                )
            }
        }
    }
}

@Composable
fun TrackSelectionPanel(
    title: String,
    tracks: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(tracks) { index, track ->
                val isCurrent = index == selectedIndex
                var isFocused by remember { mutableStateOf(false) }

                Surface(
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .onFocusChanged { isFocused = it.isFocused }
                        .border(
                            width = 1.dp,
                            color = if (isCurrent) Color(0xFFFF2E93) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isFocused -> Color(0xFFFF2E93)
                        isCurrent -> Color(0x33FF2E93)
                        else -> Color(0xFF1C1C2A)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = track,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Custom modifier helper to avoid unresolved size issues
private fun Modifier.size(size: androidx.compose.ui.unit.Dp): Modifier = this.width(size).height(size)
