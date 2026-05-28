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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
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
import com.solo.soloplayer.data.local.entity.MovieEntity
import com.solo.soloplayer.ui.viewmodel.MainViewModel

@Composable
fun MoviesLibraryScreen(
    viewModel: MainViewModel,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.movies.collectAsState()

    var selectedGenre by remember { mutableStateOf("All") }
    var selectedYear by remember { mutableStateOf("All") }
    var selectedFormat by remember { mutableStateOf("All") }

    // Predefined genres for filtering
    val genres = listOf("All", "Sci-Fi", "Drama", "Action", "Adventure")
    val years = listOf("All", "2026", "2025", "2024", "2023", "2014")
    val formats = listOf("All", "ISO", "MKV")

    // Filtered movies logic
    val filteredMovies = remember(movies, selectedGenre, selectedYear, selectedFormat) {
        movies.filter { movie ->
            val genreMatch = selectedGenre == "All" || movie.overview.contains(selectedGenre, ignoreCase = true) || movie.title.contains(selectedGenre, ignoreCase = true)
            val yearMatch = selectedYear == "All" || movie.productionYear.toString() == selectedYear
            val formatMatch = selectedFormat == "All" || movie.videoType.uppercase() == selectedFormat
            genreMatch && yearMatch && formatMatch
        }.ifEmpty {
            // Provide high-quality mock data if database is empty so design looks premium
            getMockMoviesList().filter { movie ->
                val genreMatch = selectedGenre == "All" || movie.overview.contains(selectedGenre, ignoreCase = true) || movie.title.contains(selectedGenre, ignoreCase = true)
                val yearMatch = selectedYear == "All" || movie.productionYear.toString() == selectedYear
                val formatMatch = selectedFormat == "All" || movie.videoType.uppercase() == selectedFormat
                genreMatch && yearMatch && formatMatch
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C12))
            .padding(top = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        // Top Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Movies",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp)
            )

            // Genre Selector
            FilterDropdown(
                label = "Genre",
                selectedOption = selectedGenre,
                options = genres,
                onSelected = { selectedGenre = it }
            )

            // Year Selector
            FilterDropdown(
                label = "Year",
                selectedOption = selectedYear,
                options = years,
                onSelected = { selectedYear = it }
            )

            // Format Selector
            FilterDropdown(
                label = "Format",
                selectedOption = selectedFormat,
                options = formats,
                onSelected = { selectedFormat = it }
            )
        }

        // Poster Wall Grid
        if (filteredMovies.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No movies match current filter selections.",
                    color = Color(0x66FFFFFF),
                    fontSize = 16.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMovies) { movie ->
                    MoviePosterCard(
                        movie = movie,
                        onClick = { onMovieClick(movie.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    selectedOption: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    width = 1.dp,
                    color = when {
                        isFocused -> Color(0xFFFF2E93)
                        expanded -> Color(0xFFFF2E93)
                        else -> Color(0x33FFFFFF)
                    },
                    shape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            color = if (isFocused) Color(0x15FF2E93) else Color(0xFF161622)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$label: $selectedOption",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1C1C2A))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option, color = Color.White) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MoviePosterCard(
    movie: MovieEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.08f else 1.0f, label = "PosterScale")

    Column(
        modifier = modifier
            .width(160.dp)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.67f)
                .border(
                    width = 2.dp,
                    color = if (isFocused) Color(0xFFFF2E93) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Poster Image
            AsyncImage(
                model = movie.posterUrl.ifBlank { "https://image.tmdb.org/t/p/w500/gEU2QvEzv5eU428NlZz2gC26zCN.jpg" },
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Format Badges Layer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Top format badge (e.g. ISO/MKV)
                Box(
                    modifier = Modifier
                        .background(Color(0xBBFF2E93), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = movie.videoType,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bottom badge row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Resolution Badge
                    Box(
                        modifier = Modifier
                            .background(Color(0x99000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = movie.resolution,
                            color = Color(0xFF2E93FF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // HDR Badge
                    if (movie.hdrType.isNotBlank() && movie.hdrType != "SDR") {
                        Box(
                            modifier = Modifier
                                .background(Color(0x99000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = movie.hdrType,
                                color = Color(0xFF00FF88),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = movie.title,
            color = if (isFocused) Color.White else Color(0xCCFFFFFF),
            fontSize = 14.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// Predefined mock data to ensure we display a beautiful poster wall even on empty DB
fun getMockMoviesList(): List<MovieEntity> {
    return listOf(
        MovieEntity(
            id = "1",
            title = "Dune: Part Two",
            originalTitle = "Dune: Part Two",
            overview = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
            communityRating = 8.8f,
            productionYear = 2024,
            runTimeTicks = 9960000000000,
            officialRating = "PG-13",
            posterUrl = "https://image.tmdb.org/t/p/w500/czemqn2vFrJqewRpqQzKqhqrlbS.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/original/lz5740G9v36Z62SLZ17xeaNX065.jpg",
            videoType = "MKV",
            resolution = "4K",
            hdrType = "Dolby Vision",
            audioFormat = "Atmos",
            rawFilePath = "smb://nas/movies/dune2.mkv",
            isWatched = false,
            resumeTicks = 0,
            lastSyncTime = System.currentTimeMillis()
        ),
        MovieEntity(
            id = "2",
            title = "Oppenheimer",
            originalTitle = "Oppenheimer",
            overview = "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb.",
            communityRating = 8.7f,
            productionYear = 2023,
            runTimeTicks = 10800000000000,
            officialRating = "R",
            posterUrl = "https://image.tmdb.org/t/p/w500/8Gxv2wSbsysLYlhLMVx7asw3g26.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/original/fm6KqX2IOZUN57r2UjW1Lg4JI5T.jpg",
            videoType = "ISO",
            resolution = "4K",
            hdrType = "HDR10",
            audioFormat = "TrueHD",
            rawFilePath = "smb://nas/movies/oppenheimer.iso",
            isWatched = true,
            resumeTicks = 12000000000,
            lastSyncTime = System.currentTimeMillis()
        ),
        MovieEntity(
            id = "3",
            title = "Blade Runner 2049",
            originalTitle = "Blade Runner 2049",
            overview = "A new blade runner, LAPD Officer K, unearths a long-buried secret that has the potential to plunge what's left of society into chaos.",
            communityRating = 8.0f,
            productionYear = 2017,
            runTimeTicks = 9840000000000,
            officialRating = "R",
            posterUrl = "https://image.tmdb.org/t/p/w500/gGe2uBwH6jR6m6K6QQjmA0RppSp.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/original/9r1Bw9m4gXv79oW875eS6qH2735.jpg",
            videoType = "MKV",
            resolution = "1080p",
            hdrType = "SDR",
            audioFormat = "DTS-HD",
            rawFilePath = "smb://nas/movies/bladerunner2049.mkv",
            isWatched = false,
            resumeTicks = 0,
            lastSyncTime = System.currentTimeMillis()
        ),
        MovieEntity(
            id = "4",
            title = "Avatar: The Way of Water",
            originalTitle = "Avatar: The Way of Water",
            overview = "Jake Sully lives with his newfound family formed on the extrasolar moon Pandora. Once a familiar threat returns to finish what was previously started, Jake must work with Neytiri and the army of the Na'vi race to protect their home.",
            communityRating = 7.7f,
            productionYear = 2022,
            runTimeTicks = 11520000000000,
            officialRating = "PG-13",
            posterUrl = "https://image.tmdb.org/t/p/w500/t6HI22cZqnZE7ldSRXiiR6T6NQI.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/original/t56XLE90P6i2Z5qF15Vo79T2rQd.jpg",
            videoType = "MKV",
            resolution = "4K",
            hdrType = "HDR10",
            audioFormat = "Atmos",
            rawFilePath = "smb://nas/movies/avatar2.mkv",
            isWatched = false,
            resumeTicks = 0,
            lastSyncTime = System.currentTimeMillis()
        ),
        MovieEntity(
            id = "5",
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
            rawFilePath = "smb://nas/movies/interstellar.mkv",
            isWatched = false,
            resumeTicks = 0,
            lastSyncTime = System.currentTimeMillis()
        )
    )
}
