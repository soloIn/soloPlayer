package com.solo.soloplayer.data.mapper

import com.solo.soloplayer.data.local.entity.MovieEntity
import com.solo.soloplayer.data.remote.EmbyItemDto

object MediaParser {

    fun toMovieEntity(
        item: EmbyItemDto,
        serverUrl: String,
        token: String,
        syncStartTime: Long
    ): MovieEntity {
        // Parse media streams for resolution, HDR, and audio
        val videoStream = item.MediaStreams?.firstOrNull { it.Type.equals("Video", ignoreCase = true) }
            ?: item.MediaSources?.firstOrNull()?.MediaStreams?.firstOrNull { it.Type.equals("Video", ignoreCase = true) }
        
        val width = videoStream?.Width ?: 0
        val height = videoStream?.Height ?: 0
        val resolution = if (width >= 3840 || height >= 2160) "4K" else "1080p"

        val videoRange = videoStream?.VideoRange ?: ""
        val displayTitle = videoStream?.DisplayTitle ?: ""
        val title = videoStream?.Title ?: ""
        val codec = videoStream?.Codec ?: ""

        val hdrType = when {
            videoRange.contains("Dolby Vision", ignoreCase = true) || 
            displayTitle.contains("Dolby Vision", ignoreCase = true) ||
            title.contains("Dolby Vision", ignoreCase = true) ||
            codec.contains("dovi", ignoreCase = true) ||
            codec.contains("dv", ignoreCase = true) -> "Dolby Vision"

            videoRange.contains("HDR", ignoreCase = true) || 
            displayTitle.contains("HDR", ignoreCase = true) ||
            title.contains("HDR", ignoreCase = true) -> "HDR10"

            else -> "SDR"
        }

        val audioStream = item.MediaStreams?.firstOrNull { it.Type.equals("Audio", ignoreCase = true) }
            ?: item.MediaSources?.firstOrNull()?.MediaStreams?.firstOrNull { it.Type.equals("Audio", ignoreCase = true) }
        
        val audioCodec = audioStream?.Codec ?: ""
        val audioDisplay = audioStream?.DisplayTitle ?: ""
        val audioTitle = audioStream?.Title ?: ""
        val fullAudioText = "$audioCodec $audioDisplay $audioTitle".lowercase()

        val audioFormat = when {
            fullAudioText.contains("atmos") -> "Atmos"
            fullAudioText.contains("truehd") -> "TrueHD"
            fullAudioText.contains("dts-hd") || fullAudioText.contains("dts") || fullAudioText.contains("dtshd") -> "DTS-HD"
            else -> "Unknown"
        }

        val path = item.Path ?: ""
        val container = item.MediaSources?.firstOrNull()?.Container ?: ""
        val videoType = if (container.equals("iso", ignoreCase = true) || path.endsWith(".iso", ignoreCase = true)) {
            "ISO"
        } else {
            "MKV"
        }

        val posterUrl = "${serverUrl.trimEnd('/')}/Items/${item.Id}/Images/Primary?api_key=$token"
        val backdropUrl = "${serverUrl.trimEnd('/')}/Items/${item.Id}/Images/Backdrop?api_key=$token"
        val rawFilePath = item.Path ?: item.MediaSources?.firstOrNull()?.Path ?: ""

        return MovieEntity(
            id = item.Id,
            title = item.Name,
            originalTitle = item.OriginalTitle ?: "",
            overview = item.Overview ?: "",
            communityRating = item.CommunityRating ?: 0f,
            productionYear = item.ProductionYear ?: 0,
            runTimeTicks = item.RunTimeTicks ?: 0L,
            officialRating = item.OfficialRating ?: "",
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            videoType = videoType,
            resolution = resolution,
            hdrType = hdrType,
            audioFormat = audioFormat,
            rawFilePath = rawFilePath,
            isWatched = item.UserData?.Played ?: false,
            resumeTicks = item.UserData?.PlaybackPositionTicks ?: 0L,
            lastSyncTime = syncStartTime
        )
    }
}
