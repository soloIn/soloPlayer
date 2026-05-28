package com.solo.soloplayer

import com.solo.soloplayer.data.mapper.MediaParser
import com.solo.soloplayer.data.remote.EmbyItemDto
import com.solo.soloplayer.data.remote.MediaSourceDto
import com.solo.soloplayer.data.remote.MediaStreamDto
import com.solo.soloplayer.data.remote.UserDataDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaParserTest {

    private val serverUrl = "http://192.168.1.100:8096"
    private val token = "mock_token"
    private val syncTime = 123456789L

    private fun createBaseDto(
        id: String = "1",
        name: String = "Test Movie",
        path: String? = "/path/to/movie.mkv",
        mediaStreams: List<MediaStreamDto>? = null,
        mediaSources: List<MediaSourceDto>? = null,
        userData: UserDataDto? = null
    ) = EmbyItemDto(
        Id = id,
        Name = name,
        OriginalTitle = "Original Test Movie",
        Overview = "This is a overview",
        CommunityRating = 8.5f,
        ProductionYear = 2024,
        RunTimeTicks = 1200000000L,
        OfficialRating = "PG-13",
        Path = path,
        UserData = userData,
        MediaSources = mediaSources,
        MediaStreams = mediaStreams
    )

    @Test
    fun testResolutionMapping_4K() {
        val streams = listOf(
            MediaStreamDto(Type = "Video", Codec = "hevc", DisplayTitle = "4K HEVC", Width = 3840, Height = 2160, VideoRange = "SDR", Title = null)
        )
        val dto = createBaseDto(mediaStreams = streams)
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("4K", entity.resolution)
    }

    @Test
    fun testResolutionMapping_1080p() {
        val streams = listOf(
            MediaStreamDto(Type = "Video", Codec = "h264", DisplayTitle = "1080p H264", Width = 1920, Height = 1080, VideoRange = "SDR", Title = null)
        )
        val dto = createBaseDto(mediaStreams = streams)
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("1080p", entity.resolution)
    }

    @Test
    fun testHDRMapping_DolbyVision() {
        val streams = listOf(
            MediaStreamDto(Type = "Video", Codec = "dovi", DisplayTitle = "4K HEVC HDR", Width = 3840, Height = 2160, VideoRange = "HDR", Title = "Dolby Vision")
        )
        val dto = createBaseDto(mediaStreams = streams)
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("Dolby Vision", entity.hdrType)
    }

    @Test
    fun testHDRMapping_HDR10() {
        val streams = listOf(
            MediaStreamDto(Type = "Video", Codec = "hevc", DisplayTitle = "4K HEVC HDR10", Width = 3840, Height = 2160, VideoRange = "HDR", Title = "HDR10")
        )
        val dto = createBaseDto(mediaStreams = streams)
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("HDR10", entity.hdrType)
    }

    @Test
    fun testHDRMapping_SDR() {
        val streams = listOf(
            MediaStreamDto(Type = "Video", Codec = "h264", DisplayTitle = "1080p", Width = 1920, Height = 1080, VideoRange = "SDR", Title = null)
        )
        val dto = createBaseDto(mediaStreams = streams)
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("SDR", entity.hdrType)
    }

    @Test
    fun testAudioMapping_Atmos() {
        val streams = listOf(
            MediaStreamDto(Type = "Audio", Codec = "truehd", DisplayTitle = "Dolby Atmos", Width = null, Height = null, VideoRange = null, Title = "Atmos stream")
        )
        val dto = createBaseDto(mediaStreams = streams)
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("Atmos", entity.audioFormat)
    }

    @Test
    fun testAudioMapping_TrueHD() {
        val streams = listOf(
            MediaStreamDto(Type = "Audio", Codec = "truehd", DisplayTitle = "TrueHD 7.1", Width = null, Height = null, VideoRange = null, Title = "Main")
        )
        val dto = createBaseDto(mediaStreams = streams)
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("TrueHD", entity.audioFormat)
    }

    @Test
    fun testAudioMapping_DTSHD() {
        val streams = listOf(
            MediaStreamDto(Type = "Audio", Codec = "dts", DisplayTitle = "DTS-HD Master Audio", Width = null, Height = null, VideoRange = null, Title = "Main")
        )
        val dto = createBaseDto(mediaStreams = streams)
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("DTS-HD", entity.audioFormat)
    }

    @Test
    fun testAudioMapping_Unknown() {
        val streams = listOf(
            MediaStreamDto(Type = "Audio", Codec = "aac", DisplayTitle = "AAC Stereo", Width = null, Height = null, VideoRange = null, Title = null)
        )
        val dto = createBaseDto(mediaStreams = streams)
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("Unknown", entity.audioFormat)
    }

    @Test
    fun testVideoType_ISO() {
        val dto = createBaseDto(path = "/some/file.ISO")
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("ISO", entity.videoType)
    }

    @Test
    fun testVideoType_MKV() {
        val dto = createBaseDto(path = "/some/file.mkv")
        val entity = MediaParser.toMovieEntity(dto, serverUrl, token, syncTime)
        assertEquals("MKV", entity.videoType)
    }
}
