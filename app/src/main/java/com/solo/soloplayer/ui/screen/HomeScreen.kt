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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.solo.soloplayer.data.local.entity.MediaItemEntity
import com.solo.soloplayer.data.local.entity.MovieEntity
import com.solo.soloplayer.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPlayMovie: (String) -> Unit,
    onViewDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.movies.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()

    // Select the first movie as the featured hero item
    val featuredMovie = movies.firstOrNull() ?: MovieEntity(
        id = "featured_placeholder",
        title = "Interstellar",
        originalTitle = "Interstellar",
        overview = "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
        communityRating = 8.6f,
        productionYear = 2014,
        runTimeTicks = 10140000000000,
        officialRating = "PG-13",
        posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QvEzv5eU428NlZz2gC26zCN.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/original/xJHokZbljvjC1R06J7i40H6akXe.jpg",
        videoType = "MKV",
        resolution = "4K",
        hdrType = "HDR10",
        audioFormat = "Atmos",
        rawFilePath = "",
        isWatched = false,
        resumeTicks = 0,
        lastSyncTime = 0
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C12))
    ) {
        // Hero / Featured Banner (Height: 55% of Screen)
        FeaturedBanner(
            movie = featuredMovie,
            onPlayClicked = { onPlayMovie(featuredMovie.id) },
            onDetailsClicked = { onViewDetails(featuredMovie.id) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
        )

        // Continue Watching Row (Height: 45% of Screen)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Continue Watching",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val displayList = if (continueWatching.isNotEmpty()) {
                continueWatching
            } else {
                // Placeholder/Mock data for a rich design
                listOf(
                    MediaItemEntity("1", "Dune: Part Two", "dune.mkv", "VIDEO", 9000, 3000, System.currentTimeMillis()),
                    MediaItemEntity("2", "Blade Runner 2049", "br.mkv", "VIDEO", 7200, 4800, System.currentTimeMillis() - 10000),
                    MediaItemEntity("3", "Oppenheimer", "opp.iso", "ISO", 10800, 1200, System.currentTimeMillis() - 20000)
                )
            }

            LazyRow(
                contentPadding = PaddingValues(end = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(displayList) { item ->
                    ContinueWatchingCard(
                        mediaItem = item,
                        onClick = { onPlayMovie(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturedBanner(
    movie: MovieEntity,
    onPlayClicked: () -> Unit,
    onDetailsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Fullscreen Backdrop Image
        AsyncImage(
            model = movie.backdropUrl.ifBlank { "https://image.tmdb.org/t/p/original/xJHokZbljvjC1R06J7i40H6akXe.jpg" },
            contentDescription = "Featured Backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    // Vignette Gradient: Fade to dark at bottom and left
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF0C0C12)),
                            startY = size.height * 0.4f
                        )
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0C0C12), Color.Transparent),
                            endX = size.width * 0.7f
                        )
                    )
                }
        )

        // Movie Info Overlay
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.6f)
                .padding(start = 36.dp, top = 36.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${movie.productionYear}",
                    color = Color(0xAAFFFFFF),
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
                    text = movie.resolution,
                    color = Color(0xFFFF2E93),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .border(1.dp, Color(0xFFFF2E93), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                if (movie.hdrType.isNotBlank() && movie.hdrType != "SDR") {
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Movie Title
            Text(
                text = movie.title,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Overview Text
            Text(
                text = movie.overview,
                color = Color(0xCCFFFFFF),
                fontSize = 15.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.focusGroup()
            ) {
                var isPlayFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = onPlayClicked,
                    modifier = Modifier
                        .height(48.dp)
                        .onFocusChanged { isPlayFocused = it.isFocused },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPlayFocused) Color(0xFFFF2E93) else Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isPlayFocused) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play",
                            color = if (isPlayFocused) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                var isDetailsFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = onDetailsClicked,
                    modifier = Modifier
                        .height(48.dp)
                        .onFocusChanged { isDetailsFocused = it.isFocused },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDetailsFocused) Color(0xFFFF2E93) else Color(0x2AFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Details",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    mediaItem: MediaItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.08f else 1.0f, label = "CardScale")
    val progress = if (mediaItem.duration > 0) {
        mediaItem.lastPlaybackPosition.toFloat() / mediaItem.duration.toFloat()
    } else 0f

    Surface(
        onClick = onClick,
        modifier = modifier
            .width(260.dp)
            .height(150.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = 2.dp,
                color = if (isFocused) Color(0xFFFF2E93) else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161622)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image/Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x33000000),
                                Color(0xDD000000)
                            )
                        )
                    )
            )

            // Info overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Format Tag
                Box(
                    modifier = Modifier
                        .background(Color(0x7F2E93FF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = mediaItem.mediaType,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    // Movie Title
                    Text(
                        text = mediaItem.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).toInt()}% completed",
                        color = Color(0x88FFFFFF),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFFFF2E93),
                        trackColor = Color(0x33FFFFFF)
                    )
                }
            }
        }
    }
}
