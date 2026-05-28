# 模块四：双播放引擎与流分发模块 (Dual Playback Engine & Routing)

本模块是 soloPlayer 核心的多媒体解码与渲染单元。针对发烧友级别的音画质追求（Dolby Vision / Dolby Atmos 直通源码输出）以及 ISO 光盘盘片级导航的需求，系统采用 **ExoPlayer (Media3)** 与 **libVLC for Android** 双播放核心，并由路由分配器进行逻辑控制。

---

## 1. 引擎接口设计与分发路由 (PlaybackDispatcher)

为了保证播放器界面的统一，双引擎均继承统一的 `PlayerEngine` 接口，向上提供一致的状态流。

### 1.1 统一播放器接口 `PlayerEngine`

```kotlin
interface PlayerEngine {
    val playbackState: StateFlow<PlaybackState> // 统一播放状态流
    val currentPosition: StateFlow<Long>        // 毫秒级当前进度
    val duration: StateFlow<Long>               // 总时长
    
    fun init(context: Context, surfaceView: SurfaceView)
    fun prepare(mediaUrl: String, startPositionMs: Long)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setAudioTrack(trackIndex: Int)
    fun setSubtitleTrack(trackIndex: Int)
    fun getAudioTracks(): List<AudioTrackInfo>
    fun getSubtitleTracks(): List<SubtitleTrackInfo>
    fun release()
}
```

### 1.2 路由分发器 `PlaybackDispatcher`

```kotlin
class PlaybackDispatcher @Inject constructor(
    private val exoEngine: ExoPlayerEngine,
    private val vlcEngine: LibVlcPlayerEngine
) {
    fun selectEngine(videoType: String, filePath: String): PlayerEngine {
        return when {
            // 只要是 ISO 格式，无论网络还是本地，强行分发到 libVLC 引擎以提取菜单结构
            videoType == "ISO" || filePath.endsWith(".iso", ignoreCase = true) -> vlcEngine
            // 局域网内的 SMB 协议播放，推荐走 vlcEngine (原生 SMB 解码支持更佳)
            filePath.startsWith("smb://", ignoreCase = true) -> vlcEngine
            // 其它普通 MKV/MP4 标准流媒体文件，分发到 Google Media3/ExoPlayer
            else -> exoEngine
        }
    }
}
```

---

## 2. ExoPlayer (Media3) 播放引擎实现 (标准流媒体)

当路由判断为普通 MKV/MP4 格式时启用 ExoPlayer，它针对流媒体音画质和性能进行极致配置。

### 2.1 音频直通源码输出 (Audio Passthrough)
对于杜比全景声 (Dolby Atmos)、TrueHD、DTS-HD 等次世代音轨，ExoPlayer 必须通过配置 `DefaultAudioSink` 来允许音频直通（Passthrough）：

```kotlin
val audioSink = DefaultAudioSink.Builder(context)
    .setAudioProcessorChain(DefaultAudioProcessorChain())
    .setAudioTrackCapabilitiesFilter { audioCapabilities ->
        // 允许直通播放系统支持的所有次世代格式
        audioCapabilities.supportsEncoding(AudioFormat.ENCODING_E_AC3_JOC) || // Atmos over DD+
        audioCapabilities.supportsEncoding(AudioFormat.ENCODING_DTS_HD) ||    // DTS-HD
        audioCapabilities.supportsEncoding(AudioFormat.ENCODING_TRUEHD)        // Dolby TrueHD
    }
    .build()

val renderersFactory = DefaultRenderersFactory(context)
    .setAudioSink(audioSink)

val exoPlayer = ExoPlayer.Builder(context, renderersFactory).build()
```

### 2.2 HDR 视频管道配置
* 优先选用 `MediaCodecVideoRenderer` 开启 HDR10、HDR10+、Dolby Vision 的硬件解码。
* 在配置 `SurfaceView` 或 `TextureView` 时，针对 HDR 渲染必须设置 `SurfaceView.setSecure(true)` 且在 Compose TV 的 `AndroidView` 层级设置高动态色域支持（HDR Rendering Profile）。

---

## 3. libVLC 播放引擎实现 (ISO 与 SMB 原盘)

libVLC 作为重装引擎，用于加载 DVD/蓝光原盘 ISO 镜像，并直接以 SMB/HTTP Range 头形式读取文件。

### 3.1 Gradle 依赖接入
引入 VLC Android 官方预编译的 aar：
```kotlin
dependencies {
    implementation("org.videolan.android:libvlc-all:3.6.0") // 稳定版本
}
```

### 3.2 硬件加速与启动参数配置
为了确保在电视芯片（如 Amlogic, Realtek）上流程解码 4K 蓝光 ISO 影片，配置如下启动 Arguments：

```kotlin
val options = ArrayList<String>()
options.add("-vvv") // 详细日志输出，方便 debug
options.add("--http-reconnect")
options.add("--network-caching=2000") // 缓冲设为 2s 保证 SMB 播放平滑度

// 硬件加速方案选择：MediaCodec 硬件解码
options.add("--codec=mediacodec_ndk")
options.add("--mediacodec-all-codecs")

val libVLC = LibVLC(context, options)
val mediaPlayer = MediaPlayer(libVLC)
```

### 3.3 物理路径（SMB）直接加载机制
当播放地址为 `smb://user:pass@192.168.1.100/Share/movie.iso` 时：
1. libVLC 原生搭载了 `libsmb2`（VLC 核心包中已静态编译）。
2. 我们直接通过 `Media` 对象加载此 URL：
   ```kotlin
   val media = Media(libVLC, Uri.parse(smbUrl))
   media.setHWDecoderEnabled(true, true) // 启用硬解
   mediaPlayer.media = media
   media.release()
   mediaPlayer.play()
   ```
3. **优势**：libVLC 内部的多线程 I/O 模块会自动向 SMB 服务端发起按需寻址，免去了先通过 HTTP 代理再下载的延迟。

---

## 4. 异常处理与降级机制 (Engine Error Handling)

1. **寻轨错误降级**：
   在播放蓝光 ISO 时，如果主视频轨发生加密（AACS 未能解密）或物理损坏，`vlcEngine` 触发 `Error` 状态。
   此时触发**静默降级**：
   * 弹窗提示：“该 ISO 文件解密失败或格式不支持，正在尝试用 Emby HTTP 代理模式重新打开。”
   * 将请求路由降级，调用 Emby 接口对 ISO 进行服务器端转码（Stream URL 指向 Emby 的 `/Videos/{Id}/master.m3u8`），并改由 `exoEngine` 进行播放，最大程度确保用户“能播放”。
2. **连接超时与网络恢复**：
   * 若播放 SMB 路径时发生网络中断（触发 `libvlc_MediaPlayerEncounteredError`），播放器自动暂停，显示网络异常提示，并在本地记录当前的播放位置（通过 tick 换算）。
   * 一旦网络连接恢复，用户点击重试，则从缓存的 Ticks 自动发起 `seekTo` 恢复播放。
