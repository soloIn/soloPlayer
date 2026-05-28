package com.solo.soloplayer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.solo.soloplayer.ui.components.NavigationScreen
import com.solo.soloplayer.ui.components.TVNavigationDrawer
import com.solo.soloplayer.ui.screen.HomeScreen
import com.solo.soloplayer.ui.screen.MovieDetailsScreen
import com.solo.soloplayer.ui.screen.MoviesLibraryScreen
import com.solo.soloplayer.ui.screen.SettingsScreen
import com.solo.soloplayer.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentScreen by remember { mutableStateOf(NavigationScreen.HOME) }
            var selectedMovieIdForDetails by remember { mutableStateOf<String?>(null) }

            // Main TV UI layout wrap with navigation drawer
            TVNavigationDrawer(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    // Close details view when choosing another main screen from drawer
                    selectedMovieIdForDetails = null
                    currentScreen = screen
                },
                modifier = Modifier.fillMaxSize()
            ) {
                val activeDetailsId = selectedMovieIdForDetails
                if (activeDetailsId != null) {
                    // Render details overlay if active
                    MovieDetailsScreen(
                        movieId = activeDetailsId,
                        viewModel = viewModel,
                        onPlayMovie = { id, playWithDiscMenu ->
                            startActivity(
                                PlayerActivity.createIntent(
                                    context = this@MainActivity,
                                    movieId = id,
                                    playWithDiscMenu = playWithDiscMenu
                                )
                            )
                        },
                        onBackClicked = {
                            selectedMovieIdForDetails = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Otherwise render active drawer panel
                    when (currentScreen) {
                        NavigationScreen.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onPlayMovie = { id ->
                                    startActivity(
                                        PlayerActivity.createIntent(
                                            context = this@MainActivity,
                                            movieId = id,
                                            playWithDiscMenu = false
                                        )
                                    )
                                },
                                onViewDetails = { id ->
                                    selectedMovieIdForDetails = id
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        NavigationScreen.MOVIES -> {
                            MoviesLibraryScreen(
                                viewModel = viewModel,
                                onMovieClick = { id ->
                                    selectedMovieIdForDetails = id
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        NavigationScreen.TV_SHOWS -> {
                            // Presenting MoviesLibraryScreen as placeholder TV Show library
                            MoviesLibraryScreen(
                                viewModel = viewModel,
                                onMovieClick = { id ->
                                    selectedMovieIdForDetails = id
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        NavigationScreen.LIBRARIES -> {
                            // Display settings pane focusing on folders
                            SettingsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        NavigationScreen.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
