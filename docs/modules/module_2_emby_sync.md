# 模块二：Emby 媒体库同步与缓存模块 (Emby Media Sync & Caching)

本模块是 soloPlayer 数据层 (Data Layer) 的核心。它负责**从 Emby 服务器增量拉取电影与电视剧的元数据、解析章节及媒体物理路径（如 SMB 路径），并在本地进行 Room 高速缓存**。这保证了即使在网络抖动或离线状态下，用户也能秒开海报墙，并且为双播放引擎提供即时数据支撑。

---

## 1. 业务流程与架构设计

### 1.1 增量同步架构图

```
             ┌────────────────────────────────┐
             │       WorkManager (后台任务)    │
             └───────────────┬────────────────┘
                             │ 定期或手动触发
                             ▼
             ┌────────────────────────────────┐
             │         SyncRepository         │
             └───────────────┬────────────────┘
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
   本地增量对比 (ROOM)                Emby API 请求 (Retrofit)
   1. 比较 LastSavedDateTime           1. /Users/{UserId}/Views
   2. 清理已被服务端删除的条目          2. /Items (带 MinDateLastSaved)
            │                                 │
            └────────────────┬────────────────┘
                             ▼
             ┌────────────────────────────────┐
             │      本地 Room 数据库写入       │
             └────────────────────────────────┘
```

### 1.2 核心业务规则
1. **同步触发机制**：
   * **后台定时同步**：由 Android `WorkManager` 接管，依据设置中的频率选项（如“每30分钟”、“每1小时”）在后台默默执行。
   * **手动下拉/按键刷新**：在主页或电影墙页面按刷新键，立即启动单次同步协程。
2. **增量对比策略**：
   为了防止每次同步下载几千条元数据，每次同步成功后在本地 SharedPreferences 记录 `last_sync_timestamp`。
   在下次同步时，向 Emby API 传入 `MinDateLastSaved = last_sync_timestamp`。仅请求在此时间点之后变更或新增的数据，极大地节省了宽带与 CPU 开销。
3. **数据失效与删除同步**：
   * 在增量请求中，比对本地库中的 Item ID 与服务端最新的 ID 集合，若服务端已删除该影片，本地 Room 数据库执行级联物理删除，确保数据一致性。

---

## 2. Room 本地数据库设计 (Room Schema)

为了能够展示海报墙、原盘格式（ISO/MKV）、音效（Atmos）、HDR 类型并缓存章节信息，设计如下数据表：

### 2.1 电影数据表 `movies`

```kotlin
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: String,          // Emby ItemId
    val title: String,                   // 电影中文名/主标题
    val originalTitle: String?,          // 原始标题
    val overview: String?,               // 剧情梗概
    val communityRating: Float?,         // 社区评分 (例如 8.8)
    val productionYear: Int?,            // 上映年份
    val runTimeTicks: Long?,             // 时长 (Ticks)
    val officialRating: String?,         // 分级 (如 PG-13)
    val posterUrl: String?,              // 海报图片地址 (HTTP 或 SMB 缓存图)
    val backdropUrl: String?,            // 背景大图地址
    val videoType: String,               // 视频容器格式 (ISO / MKV / MP4)
    val resolution: String,              // 分辨率 (4K / 1080p / 720p)
    val hdrType: String?,                // HDR类型 (HDR10 / Dolby Vision / SDR)
    val audioFormat: String?,            // 音频格式 (Atmos / DTS-HD / AC3)
    val rawFilePath: String,             // 原始路径 (SMB 路径或 HTTP 流路径)
    val isWatched: Boolean = false,      // 是否已观看
    val resumeTicks: Long = 0L,          // 历史观看断点 ticks
    val lastSyncTime: Long = System.currentTimeMillis()
)
```

### 2.2 章节数据表 `chapters`

用于在电影详情页及播放器 `DISC MENU` 快速渲染章节卡片，采用外键关联 `movies` 表：

```kotlin
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE // 电影被删，章节级联删除
        )
    ],
    indices = [Index(value = ["movieId"])]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val movieId: String,
    val name: String,                    // 章节名称
    val startPositionTicks: Long,        // 该章节起始 ticks
    val thumbnailUrl: String?            // 章节预览图 URL
)
```

---

## 3. Emby 同步 API 接口定义与解析

### 3.1 Retrofit 服务定义

```kotlin
interface EmbySyncApi {
    
    // 1. 获取用户的媒体库视图 (用于确定哪些是电影库、电视剧库)
    @GET("emby/Users/{userId}/Views")
    suspend fun getUserViews(
        @Path("userId") userId: String,
        @Header("X-Emby-Token") token: String
    ): EmbyResponse<List<LibraryFolderDto>>

    // 2. 增量获取电影列表
    @GET("emby/Items")
    suspend fun getLibraryItems(
        @Header("X-Emby-Token") token: String,
        @Query("ParentId") parentId: String,       // 媒体库 FolderId
        @Query("Recursive") recursive: Boolean = true,
        @Query("IncludeItemTypes") itemTypes: String = "Movie",
        @Query("Fields") fields: String = "Path,Overview,Genres,CommunityRating,RunTimeTicks,ProductionYear,OfficialRating,MediaSources,MediaStreams",
        @Query("MinDateLastSaved") minDateSaved: String? = null // 用于增量同步的核心时间戳
    ): EmbyResponse<List<LibraryItemDto>>

    // 3. 获取特定影片的详细章节 (Chapters)
    @GET("emby/Items/{itemId}/PlaybackInfo")
    suspend fun getPlaybackInfo(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Header("X-Emby-Token") token: String
    ): PlaybackInfoResponse
}
```

### 3.2 媒体流与原盘标记提取逻辑

在同步接收到 `LibraryItemDto` 后，客户端需要解析 `MediaSources` 和 `MediaStreams` 来提取发烧友用户关心的角标标签：

```kotlin
fun parseMediaMetadata(dto: LibraryItemDto): ParsedMedia {
    val source = dto.mediaSources?.firstOrNull()
    val rawPath = source?.path ?: ""
    
    // 1. 提取视频格式 (ISO vs MKV)
    val videoType = when {
        rawPath.endsWith(".iso", ignoreCase = true) -> "ISO"
        source?.container?.contains("iso", ignoreCase = true) == true -> "ISO"
        else -> "MKV"
    }

    // 2. 提取分辨率
    val videoStream = source?.mediaStreams?.firstOrNull { it.type == "Video" }
    val resolution = when {
        (videoStream?.width ?: 0) >= 3840 -> "4K"
        (videoStream?.width ?: 0) >= 1920 -> "1080p"
        else -> "720p"
    }

    // 3. 提取 HDR 格式
    val hdrType = when {
        videoStream?.videoLayout?.contains("dv", ignoreCase = true) == true -> "Dolby Vision"
        videoStream?.videoLayout?.contains("hdr10", ignoreCase = true) == true -> "HDR10"
        else -> "SDR"
    }

    // 4. 提取音频源码格式 (DTS-HD/TrueHD Atmos/DTS)
    val audioStream = source?.mediaStreams?.firstOrNull { it.type == "Audio" }
    val audioFormat = when {
        audioStream?.profile?.contains("atmos", ignoreCase = true) == true -> "Dolby Atmos"
        audioStream?.codec?.contains("truehd", ignoreCase = true) == true -> "Dolby TrueHD"
        audioStream?.codec?.contains("dts", ignoreCase = true) == true -> "DTS-HD"
        else -> audioStream?.codec?.uppercase() ?: "AAC"
    }

    return ParsedMedia(videoType, resolution, hdrType, audioFormat, rawPath)
}
```

---

## 4. 后台任务调度与可靠性保证 (WorkManager)

### 4.1 SyncWorker 实现
* 每次执行同步时，利用 Android 的 `ConnectivityManager` 先行检查网络状态，仅在局域网 Wi-Fi/以太网处于“未计量连接”（Unmetered Network）状态下执行大体积海报图的高速缓存，防止消耗用户移动网络数据。
* **Room 事务保证**：在写入本地 `movies` 表与级联写入 `chapters` 表时，使用 `@Transaction` 包装 DAO 方法。如果中途发生断网导致获取某一电影的章节接口失败，回滚当前电影的数据库写入，保证本地缓存状态的数据一致性，不出现无章节的“半残”电影卡片。
