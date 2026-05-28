# 模块六：播放状态同步与心跳模块 (Playstate Sync & Heartbeats)

本模块充当 soloPlayer 与 Emby 服务器之间“进度粘合剂”的角色。无论是远程串流还是通过 SMB 直连播放 ISO，**客户端必须实时、稳定地上报当前的精确播放位置，确保在其它设备上（如手机、网页端）能无缝“继续观看”**。同时，模块提供了网络中断时的本地断点缓存和断网重连同步功能。

---

## 1. 播放状态同步时序与 API

### 1.1 状态同步流程图

```
      [播放开始]                     [播放中 (10s心跳)]               [播放结束/返回]
          │                                  │                              │
          ▼                                  ▼                              ▼
  POST /Sessions/Playing           POST /Sessions/Playing/Progress   POST /Sessions/Playing/Stopped
          │                                  │                              │
  创建播放 Session,                更新播放状态 (ticks,暂停状态),      注销 Session, 
  记录 PlaySessionId                支持实时多端断点记忆              最终保存当前观看百分比
```

### 1.2 关键参数计算：时间单位转换 (Ticks)
Emby API 的时间单位采用 **Ticks**（1 秒 = 10,000,000 Ticks，即 **1 毫秒 = 10,000 Ticks**）。
在客户端上报时，必须对底层播放引擎 (VLC 或 ExoPlayer) 返回的毫秒数进行转换：

```kotlin
fun convertMsToTicks(positionMs: Long): Long {
    return positionMs * 10_000L
}

fun convertTicksToMs(ticks: Long): Long {
    return ticks / 10_000L
}
```

---

## 2. 心跳上报服务设计 (Heartbeat Service)

为了确保进程即使在后台或播放 Activity 重建时也能持续安全地上报进度，心跳模块作为 **Android 绑定 Service (Bound Service) 配合协程**实现。

### 2.1 核心服务逻辑

```kotlin
class PlaybackSyncService : Service() {
    private var syncJob: Job? = null
    private val client = RetrofitClient.embyApi
    
    // 启动心跳同步协程
    fun startProgressHeartbeat(itemId: String, playSessionId: String, engine: PlayerEngine) {
        syncJob?.cancel()
        syncJob = CoroutineScope(Dispatchers.IO).launch {
            // 1. 发起 PlaybackStarted 登记
            reportPlayState(PlayStateEvent.START, itemId, playSessionId, 0L)
            
            while (isActive) {
                delay(10_000L) // 每 10 秒上报一次
                val currentPositionMs = engine.currentPosition.value
                val isPaused = engine.playbackState.value is PlaybackState.Paused
                
                reportPlayState(
                    event = PlayStateEvent.PROGRESS,
                    itemId = itemId,
                    playSessionId = playSessionId,
                    positionMs = currentPositionMs,
                    isPaused = isPaused
                )
            }
        }
    }

    fun stopProgressHeartbeat(itemId: String, playSessionId: String, lastPositionMs: Long) {
        syncJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            reportPlayState(PlayStateEvent.STOP, itemId, playSessionId, lastPositionMs)
        }
    }
}
```

* **即时事件触发上报**：除了 10s 循环外，若引擎抛出 `Pause`、`Resume` 或用户进行了大幅度 `Seek` 操作，必须**立即打断当前 10s 挂起并触发一次上报**，以防止由于异常关机导致大跨度进度丢失。

---

## 3. 离线断点缓存与重连同步 (Offline Buffer)

当使用局域网 SMB 直连播放 ISO 时，虽然影片能本地读取解码，但若此时与外部 Emby 服务器连接发生抖动，会导致心跳请求超时 (`504 Gateway Timeout` 或 `ConnectException`)。

### 3.1 本地离线缓存表 `offline_playstates`
如果心跳接口发生网络异常，客户端将当前的进度记录存储到本地 Room 表中：

```kotlin
@Entity(tableName = "offline_playstates")
data class OfflinePlaystateEntity(
    @PrimaryKey val itemId: String,
    val positionTicks: Long,
    val isWatched: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
```

### 3.2 离线同步 DAO 与同步机制
```kotlin
@Dao
interface OfflinePlaystateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // 重复则覆盖为最新进度
    suspend fun saveOfflineProgress(state: OfflinePlaystateEntity)

    @Query("SELECT * FROM offline_playstates")
    suspend fun getAllOfflineProgress(): List<OfflinePlaystateEntity>

    @Query("DELETE FROM offline_playstates WHERE itemId = :itemId")
    suspend fun deleteProgressById(itemId: String)
}
```

* **重连同步工作流 (WorkManager)**：
  1. 使用广播监听器或 `NetworkCallback` 监听系统网络恢复事件。
  2. 一旦网络恢复，触发 `OfflineSyncWorker` 后台任务。
  3. Worker 从本地读取 `offline_playstates` 列表，并发依次调用 `POST /Sessions/Playing/Progress` 和 `/Sessions/Playing/Stopped` 将断网期间的最新断点推送到 Emby。
  4. 同步成功后，清除本地对应的缓存记录，保障多端断点续播的准确度。
