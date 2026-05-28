package com.solo.soloplayer.data.remote

import retrofit2.http.*

data class AuthenticateByNameRequest(
    val Username: String,
    val Pw: String
)

data class UserDto(
    val Id: String,
    val Name: String
)

data class AuthenticateByNameResponse(
    val User: UserDto,
    val AccessToken: String
)

data class ViewsResponse(
    val Items: List<LibraryViewDto>
)

data class LibraryViewDto(
    val Id: String,
    val Name: String,
    val CollectionType: String?
)

data class ItemsResponse(
    val Items: List<EmbyItemDto>
)

data class EmbyItemDto(
    val Id: String,
    val Name: String,
    val OriginalTitle: String?,
    val Overview: String?,
    val CommunityRating: Float?,
    val ProductionYear: Int?,
    val RunTimeTicks: Long?,
    val OfficialRating: String?,
    val Path: String?,
    val UserData: UserDataDto?,
    val MediaSources: List<MediaSourceDto>?,
    val MediaStreams: List<MediaStreamDto>?
)

data class UserDataDto(
    val PlaybackPositionTicks: Long?,
    val PlaybackPositionTicksAtLastSave: Long?,
    val Played: Boolean?
)

data class MediaSourceDto(
    val Container: String?,
    val Path: String?,
    val Size: Long?,
    val MediaStreams: List<MediaStreamDto>?
)

data class MediaStreamDto(
    val Type: String, // "Video" or "Audio"
    val Codec: String?,
    val DisplayTitle: String?,
    val Width: Int?,
    val Height: Int?,
    val VideoRange: String?, // e.g. "HDR" or "SDR"
    val Title: String?
)

data class PlaybackInfoResponse(
    val MediaSources: List<MediaSourcePlaybackDto>?
)

data class MediaSourcePlaybackDto(
    val Id: String,
    val Chapters: List<ChapterDto>?
)

data class ChapterDto(
    val Name: String,
    val StartPositionTicks: Long,
    val ImageTag: String?
)

interface EmbyApi {
    @POST
    suspend fun authenticateByName(
        @Url url: String,
        @Body request: AuthenticateByNameRequest,
        @Header("X-Emby-Authorization") authHeader: String
    ): AuthenticateByNameResponse

    @GET
    suspend fun getLibraryViews(
        @Url url: String,
        @Header("X-Emby-Token") token: String
    ): ViewsResponse

    @GET
    suspend fun getItems(
        @Url url: String,
        @Header("X-Emby-Token") token: String,
        @QueryMap options: Map<String, String>
    ): ItemsResponse

    @GET
    suspend fun getPlaybackInfo(
        @Url url: String,
        @Header("X-Emby-Token") token: String,
        @Query("UserId") userId: String
    ): PlaybackInfoResponse

    @POST
    suspend fun reportPlaybackStart(
        @Url url: String,
        @Header("X-Emby-Token") token: String,
        @Body request: PlaybackProgressRequest
    ): retrofit2.Response<Unit>

    @POST
    suspend fun reportPlaybackProgress(
        @Url url: String,
        @Header("X-Emby-Token") token: String,
        @Body request: PlaybackProgressRequest
    ): retrofit2.Response<Unit>

    @POST
    suspend fun reportPlaybackStopped(
        @Url url: String,
        @Header("X-Emby-Token") token: String,
        @Body request: PlaybackProgressRequest
    ): retrofit2.Response<Unit>
}

data class PlaybackProgressRequest(
    val ItemId: String,
    val MediaSourceId: String?,
    val PositionTicks: Long?,
    val IsPaused: Boolean?,
    val IsMuted: Boolean?,
    val VolumeLevel: Int?,
    val EventName: String?,
    val PlayMethod: String? = "DirectPlay",
    val AudioStreamIndex: Int? = null,
    val SubtitleStreamIndex: Int? = null
)

