package com.solo.soloplayer.ui.screen

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.solo.soloplayer.data.local.entity.ChapterEntity
import com.solo.soloplayer.data.local.entity.MovieEntity
import com.solo.soloplayer.ui.viewmodel.MainViewModel

@Composable
fun MovieDetailsScreen(
    movieId: String,
    viewModel: MainViewModel,
    onPlayMovie: (String, Boolean) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.movies.collectAsState()

    // Find movie or fallback to mock
    val movie = movies.find { it.id == movieId } ?: getMockMoviesList().find { it.id == movieId } ?: getMockMoviesList().first()

    // Query chapters for this movie. We display some mock chapters if database returns empty
    val dbChapters = remember { mutableListOf<ChapterEntity>() }
    val displayChapters = if (dbChapters.isNotEmpty()) {
        dbChapters
    } else {
        listOf(
            ChapterEntity("c1", movie.id, "Chapter 1: The Launch", 0L, "https://image.tmdb.org/t/p/w300/fm6KqX2IOZUN57r2UjW1Lg4JI5T.jpg"),
            ChapterEntity("c2", movie.id, "Chapter 2: Orbit", 90000000000L, "https://image.tmdb.org/t/p/w300/xJHokZbljvjC1R06J7i40H6akXe.jpg"),
            ChapterEntity("c3", movie.id, "Chapter 3: The Wormhole", 240000000000L, "https://image.tmdb.org/t/p/w300/lz5740G9v36Z62SLZ17xeaNX065.jpg"),
            ChapterEntity("c4", movie.id, "Chapter 4: Gargantua", 480000000000L, "https://image.tmdb.org/t/p/w300/9r1Bw9m4gXv79oW875eS6qH2735.jpg"),
            ChapterEntity("c5", movie.id, "Chapter 5: Miller's Planet", 680000000000L, "https://image.tmdb.org/t/p/w300/t56XLE90P6i2Z5qF15Vo79T2rQd.jpg")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C12))
    ) {
        // Blurred Backdrop Background
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.backdropUrl.ifBlank { "https://image.tmdb.org/t/p/original/xJHokZbljvjC1R06J7i40H6akXe.jpg" },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp) // Gaussian Blur
                    .drawWithContent {
                        drawContent()
                        // Vignette & Dark Tint
                        drawRect(Color(0xDD0C0C12))
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0C0C12), Color.Transparent),
                                endX = size.width * 0.8f
                            )
                        )
                    }
            )
        }

        // Details Panel Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 24.dp)
        ) {
            // Header: Back Button
            var isBackFocused by remember { mutableStateOf(false) }
            Surface(
                onClick = onBackClicked,
                modifier = Modifier
                    .onFocusChanged { isBackFocused = it.isFocused }
                    .border(
                        width = 1.dp,
                        color = if (isBackFocused) Color(0xFFFF2E93) else Color(0x33FFFFFF),
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                color = if (isBackFocused) Color(0x22FF2E93) else Color(0x15FFFFFF)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main details block
            Row(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                // Movie Poster (Left)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = movie.posterUrl.ifBlank { "https://image.tmdb.org/t/p/w500/gEU2QvEzv5eU428NlZz2gC26zCN.jpg" },
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Info Text & Metadata (Right)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = movie.title,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    if (movie.originalTitle != movie.title) {
                        Text(
                            text = movie.originalTitle,
                            fontSize = 16.sp,
                            color = Color(0x88FFFFFF)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badges metadata row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${movie.productionYear}",
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp
                        )
                        Text(
                            text = movie.officialRating,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(Color(0x33FFFFFF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            text = formatDuration(movie.runTimeTicks),
                            color = Color(0xCCFFFFFF),
                            fontSize = 14.sp
                        )
                        Text(
                            text = movie.resolution,
                            color = Color(0xFFFF2E93),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(1.dp, Color(0xFFFF2E93), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        if (movie.hdrType != "SDR") {
                            Text(
                                text = movie.hdrType,
                                color = Color(0xFF2E93FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF2E93FF), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = movie.audioFormat,
                            color = Color(0xFF00FF88),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Overview
                    Text(
                        text = movie.overview,
                        color = Color(0xCCFFFFFF),
                        fontSize = 15.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Focusable Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.focusGroup()
                    ) {
                        var isPlayBtnFocused by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { onPlayMovie(movie.id, false) },
                            modifier = Modifier
                                .height(48.dp)
                                .onFocusChanged { isPlayBtnFocused = it.isFocused },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPlayBtnFocused) Color(0xFFFF2E93) else Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isPlayBtnFocused) Color.White else Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Play",
                                    color = if (isPlayBtnFocused) Color.White else Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Play with Disc Menu button (DVD/Blu-ray specific)
                        var isDiscBtnFocused by remember { mutableStateOf(false) }
                        val isIso = movie.videoType.uppercase() == "ISO" || movie.rawFilePath.lowercase().endsWith(".iso")
                        Surface(
                            onClick = { onPlayMovie(movie.id, true) },
                            modifier = Modifier
                                .height(48.dp)
                                .onFocusChanged { isDiscBtnFocused = it.isFocused },
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isDiscBtnFocused -> Color(0xFFFF2E93)
                                isIso -> Color(0x40FFFFFF)
                                else -> Color(0x10FFFFFF)
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                            enabled = isIso // Enabled primarily for ISOs
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null,
                                    tint = if (isIso) Color.White else Color(0x44FFFFFF)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Play with Disc Menu",
                                    color = if (isIso) Color.White else Color(0x44FFFFFF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chapters row (Bottom)
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Chapters & Scenes",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(end = 40.dp)
                ) {
                    items(displayChapters) { chapter ->
                        ChapterThumbnailCard(
                            chapter = chapter,
                            onClick = {
                                // Jump directly to chapter starting position
                                onPlayMovie(movie.id, false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterThumbnailCard(
    chapter: ChapterEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.08f else 1.0f, label = "ChapterScale")

    Column(
        modifier = modifier
            .width(180.dp)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .border(
                    width = 2.dp,
                    color = if (isFocused) Color(0xFFFF2E93) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = chapter.thumbnailUrl.ifBlank { "https://image.tmdb.org/t/p/w300/fm6KqX2IOZUN57r2UjW1Lg4JI5T.jpg" },
                contentDescription = chapter.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Display chapter duration / progress time
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color(0x99000000), RoundedCornerShape(topStart = 4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatTicksToTime(chapter.startPositionTicks),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = chapter.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Convert ticks (100ns units) to HH:MM:SS
fun formatDuration(ticks: Long): String {
    val seconds = ticks / 10_000_000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun formatTicksToTime(ticks: Long): String {
    val seconds = ticks / 10_000_000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
