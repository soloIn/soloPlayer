package com.solo.soloplayer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solo.soloplayer.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsCategory {
    MEDIA_SYNC,
    SMB_LIBRARY,
    EMBY_SERVERS
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableStateOf(SettingsCategory.MEDIA_SYNC) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C12))
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left Column: Categories List (Width: 30%)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.3f)
                .background(Color(0xFF14141E), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            )

            SettingsCategory.values().forEach { category ->
                val isSelected = category == activeCategory
                var isFocused by remember { mutableStateOf(false) }

                Surface(
                    onClick = { activeCategory = category },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .onFocusChanged { isFocused = it.isFocused },
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isFocused -> Color(0xFFFF2E93)
                        isSelected -> Color(0x22FFFFFF)
                        else -> Color.Transparent
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = when (category) {
                                SettingsCategory.MEDIA_SYNC -> "Media Sync"
                                SettingsCategory.SMB_LIBRARY -> "SMB Library"
                                SettingsCategory.EMBY_SERVERS -> "Emby Servers"
                            },
                            color = if (isFocused) Color.White else Color(0xCCFFFFFF),
                            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Right Column: Details Panel (Width: 70%)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.7f)
                .background(Color(0xFF14141E), RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            when (activeCategory) {
                SettingsCategory.MEDIA_SYNC -> MediaSyncSettingsPanel(viewModel)
                SettingsCategory.SMB_LIBRARY -> SmbLibrarySettingsPanel(viewModel)
                SettingsCategory.EMBY_SERVERS -> EmbyServersSettingsPanel(viewModel)
            }
        }
    }
}

@Composable
fun MediaSyncSettingsPanel(viewModel: MainViewModel) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncInterval by viewModel.syncInterval.collectAsState()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val formattedTime = if (lastSyncTime > 0) dateFormat.format(Date(lastSyncTime)) else "Never"

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Media Sync Settings",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Status Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C2A), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(text = "Sync Status", color = Color(0x88FFFFFF), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF2E93),
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(text = "Syncing media collections...", color = Color.White, fontSize = 15.sp)
                } else {
                    Text(text = "Idle", color = Color(0xFF00FF88), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Last Synced: $formattedTime", color = Color(0xCCFFFFFF), fontSize = 14.sp)
        }

        // Manual Sync Button
        var isSyncFocused by remember { mutableStateOf(false) }
        Surface(
            onClick = { viewModel.triggerManualSync() },
            enabled = !isSyncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .onFocusChanged { isSyncFocused = it.isFocused },
            shape = RoundedCornerShape(8.dp),
            color = if (isSyncFocused) Color(0xFFFF2E93) else Color(0x11FFFFFF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sync Library Now",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Sync Intervals
        Column {
            Text(
                text = "Background Sync Interval",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.focusGroup()
            ) {
                listOf(15, 30, 60, 360, 1440).forEach { interval ->
                    val isCurrent = syncInterval == interval
                    var isFocused by remember { mutableStateOf(false) }

                    Surface(
                        onClick = { viewModel.setSyncInterval(interval) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .onFocusChanged { isFocused = it.isFocused },
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isFocused -> Color(0xFFFF2E93)
                            isCurrent -> Color(0x33FF2E93)
                            else -> Color(0xFF1C1C2A)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isCurrent) Color(0xFFFF2E93) else Color.Transparent
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = when (interval) {
                                    15 -> "15m"
                                    30 -> "30m"
                                    60 -> "1h"
                                    360 -> "6h"
                                    else -> "24h"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmbLibrarySettingsPanel(viewModel: MainViewModel) {
    val smbAccounts by viewModel.smbAccounts.collectAsState()
    var inputPath by remember { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SMB Storage Libraries",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // List of SMB Paths
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (smbAccounts.isEmpty()) {
                item {
                    Text(
                        text = "No SMB folders connected. Add one below.",
                        color = Color(0x66FFFFFF),
                        fontSize = 14.sp
                    )
                }
            } else {
                items(smbAccounts) { account ->
                    var isItemFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1C2A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = account.shareName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = account.serverAddress, color = Color(0x88FFFFFF), fontSize = 12.sp)
                        }

                        // Delete button
                        var isDeleteFocused by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { viewModel.removeSmbFolder(account.id) },
                            modifier = Modifier
                                .onFocusChanged { isDeleteFocused = it.isFocused }
                                .size(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDeleteFocused) Color(0xFFFF2D55) else Color(0x1AFFFFFF)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add SMB Mock Input Button
        Surface(
            onClick = {
                viewModel.addSmbFolder("smb://192.168.1.100/Media/Movies", "NAS Movies share")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .onFocusChanged { isInputFocused = it.isFocused },
            shape = RoundedCornerShape(8.dp),
            color = if (isInputFocused) Color(0xFFFF2E93) else Color(0xFF1C1C2A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add SMB Path (NAS Movie share)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmbyServersSettingsPanel(viewModel: MainViewModel) {
    val embyServers by viewModel.embyServers.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Emby Server Configurations",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // List of Emby Servers
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (embyServers.isEmpty()) {
                item {
                    Text(
                        text = "No Emby Servers configured. Add one below.",
                        color = Color(0x66FFFFFF),
                        fontSize = 14.sp
                    )
                }
            } else {
                items(embyServers) { server ->
                    var isServerFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1C2A), RoundedCornerShape(8.dp))
                            .clickable { viewModel.connectEmbyServer(server.id) }
                            .onFocusChanged { isServerFocused = it.isFocused }
                            .border(
                                width = 1.dp,
                                color = if (isServerFocused) Color(0xFFFF2E93) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = server.serverName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = server.serverUrl, color = Color(0x88FFFFFF), fontSize = 12.sp)
                        }

                        // Connection State Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (server.isConnected) Color(0xFF00FF88) else Color(0x44FFFFFF),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (server.isConnected) "Connected" else "Connect",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Delete Server Button
                            var isDeleteFocused by remember { mutableStateOf(false) }
                            Surface(
                                onClick = { viewModel.removeEmbyServer(server.id) },
                                modifier = Modifier
                                    .onFocusChanged { isDeleteFocused = it.isFocused }
                                    .size(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDeleteFocused) Color(0xFFFF2D55) else Color(0x1AFFFFFF)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Emby Server Mock Button
        var isAddFocused by remember { mutableStateOf(false) }
        Surface(
            onClick = {
                viewModel.addEmbyServer("http://192.168.1.101:8096", "Home Emby Server")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .onFocusChanged { isAddFocused = it.isFocused },
            shape = RoundedCornerShape(8.dp),
            color = if (isAddFocused) Color(0xFFFF2E93) else Color(0xFF1C1C2A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Emby Server (Home server)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Custom modifier helper to avoid unresolved size issues
private fun Modifier.size(size: androidx.compose.ui.unit.Dp): Modifier = this.width(size).height(size)
