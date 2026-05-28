package com.solo.soloplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NavigationScreen(val displayName: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    MOVIES("Movies", Icons.Default.PlayArrow),
    TV_SHOWS("TV Shows", Icons.Default.List),
    LIBRARIES("Libraries", Icons.Default.Refresh),
    SETTINGS("Settings", Icons.Default.Settings)
}


@Composable
fun TVNavigationDrawer(
    currentScreen: NavigationScreen,
    onScreenSelected: (NavigationScreen) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val drawerWidth by animateDpAsState(
        targetValue = if (isExpanded) 240.dp else 80.dp,
        label = "DrawerWidth"
    )

    Row(modifier = modifier.fillMaxSize().background(Color(0xFF0F0F15))) {
        // Drawer Column
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .onFocusChanged { focusState ->
                    isExpanded = focusState.hasFocus
                }
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xEE14141E),
                            Color(0xCC181825)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0x1AFFFFFF),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(vertical = 24.dp, horizontal = 12.dp),
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / App Title
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "App Logo",
                        tint = Color(0xFFFF2E93),
                        modifier = Modifier.size(32.dp)
                    )
                    if (isExpanded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "soloPlayer",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Menu Items
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
            ) {
                NavigationScreen.values().forEach { screen ->
                    val isSelected = screen == currentScreen
                    var isFocused by remember { mutableStateOf(false) }

                    Surface(
                        onClick = { onScreenSelected(screen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .onFocusChanged { isFocused = it.isFocused }
                            .clip(RoundedCornerShape(8.dp)),
                        color = when {
                            isFocused -> Color(0x33FFFF2E)
                            isSelected -> Color(0x1AFFFFFF)
                            else -> Color.Transparent
                        },
                        tonalElevation = if (isFocused) 8.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.displayName,
                                tint = when {
                                    isFocused -> Color(0xFFFF2E93)
                                    isSelected -> Color.White
                                    else -> Color(0x88FFFFFF)
                                }
                            )
                            if (isExpanded) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = screen.displayName,
                                    color = when {
                                        isFocused -> Color.White
                                        isSelected -> Color.White
                                        else -> Color(0x88FFFFFF)
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Footer / Profile or Version
            if (isExpanded) {
                Text(
                    text = "v1.0.0",
                    color = Color(0x44FFFFFF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Screen Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content()
        }
    }
}

// Custom modifier helper to avoid unresolved size issues
private fun Modifier.size(size: androidx.compose.ui.unit.Dp): Modifier = this.width(size).height(size)
