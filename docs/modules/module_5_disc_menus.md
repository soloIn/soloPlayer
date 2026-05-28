# 模块五：自定义光盘菜单与遥控器交互覆盖层模块 (Disc Menu & Remote Overlay)

本模块专为解决家庭影院发烧友对 **DVD 原生菜单导航** 以及 **蓝光 ISO 原生简易导航（Native Overlay）** 的交互痛点而设计。结合 TV 遥控器特点，实现无缝的 D-Pad 捕获和统一的现代 UI 覆盖层。

---

## 1. DVD 原生菜单遥控器导航 (libVLC 映射)

DVD 原生原盘菜单一般是由视频轨上的静态图像和子画面按钮组成的交互层。在 libVLC 解码播放 DVD 映像时，我们必须将 Android TV 遥控器的按键事件拦截，并直接路由到 libVLC 底层的导航接口。

### 1.1 按键映射逻辑

在承载播放的 `PlaybackActivity` 中重写 `onKeyDown`，拦截遥控器按键：

```kotlin
override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (isDvdPlaying && isShowingDvdMenu) {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                mediaPlayer.navigate(MediaPlayer.Navigate.UP)
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                mediaPlayer.navigate(MediaPlayer.Navigate.DOWN)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                mediaPlayer.navigate(MediaPlayer.Navigate.LEFT)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                mediaPlayer.navigate(MediaPlayer.Navigate.RIGHT)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                mediaPlayer.navigate(MediaPlayer.Navigate.ACTIVATE) // 激活/确认选中的 DVD 菜单按钮
                return true
            }
        }
    }
    return super.onKeyDown(keyCode, event)
}
```

---

## 2. 蓝光 ISO 简易导航原生 Overlay (DISC MENU)

针对不支持完整 Java BD-J 菜单的蓝光 ISO，我们自动解析 BDMV 结构并构造**一个完全原生的 Compose TV 交互浮窗**，提供与商业播放机一致的质感。

### 2.1 悬浮窗界面设计
浮窗（如原型图 Image 2 所示）分为两个核心区：
1. **主列表（DISC MENU）**：垂直居中的卡片堆叠（Play, Scene Selection, Audio, Subtitles）。
2. **副展板（右侧/下方弹出）**：
   * **Scene Selection（场景选择）**：水平滑动的章节卡片，包含章节名、起始时间以及本地解析的章节缩略图。
   * **Audio（音轨选择）**：展示当前 ISO 包含的所有音轨列表，高亮当前选中的次世代音轨（如：Dolby Atmos 7.1）。
   * **Subtitles（字幕选择）**：展示内置的所有 PGS（图形字幕）或外部挂载的 SRT/ASS 字幕。

### 2.2 章节解析与缩略图提取技术
如何为远程 ISO 提取章节图：
1. 客户端通过 libVLC 获取蓝光播放列表中的 Chapters 时标。
2. 在后台协程中，使用 VLC 播放器的精确定位截图功能：
   ```kotlin
   // 伪代码：在非渲染窗口下，快速截取各章节起点的单帧图作为本地缓存图
   for (chapter in chapters) {
       mediaPlayer.setTime(chapter.offsetMs)
       val bitmap = mediaPlayer.getCurrentVideoFrame()
       saveToCache(bitmap, "${movieId}_ch_${chapter.id}.jpg")
   }
   ```
   *注：仅在非播放的核心预加载协程中执行，且如果 Emby 服务端已经提供了章节图，则优先读取 Emby HTTP 章节图以提升速度。*

---

## 3. 焦点锁定 (Focus Trap) 与覆盖层生命周期

多层 Overlay 的管理是 TV 开发的重中之重。必须严格防止用户按 Down 键时，焦点意外穿透到背景视频控制栏或退出页面。

### 3.1 焦点锁在 Compose 中的实现
通过 `FocusRequester` 以及拦截 Key 事件来锁定焦点范围：

```kotlin
@Composable
fun DiscMenuOverlay(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .onPreviewKeyEvent { keyEvent ->
                // 如果按 Back 键，关闭当前 DISC MENU 覆盖层
                if (keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyDown) {
                    onDismissRequest()
                    true
                } else {
                    false
                }
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .focusRequester(focusRequester)
                .focusable()
        ) {
            content()
        }
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus() // 弹出时自动锁定强焦点
    }
}
```

### 3.2 覆盖层多级回退机制与自动淡出
1. **多级回退机制 (D-Pad Back Key)**：
   * 状态 1：如果开启了“Scene Selection”章节子列表，按 **Back 键**首先将焦点退回到主 DISC MENU 列表中，关闭章节子列表。
   * 状态 2：如果仅显示主 DISC MENU，按 **Back 键**直接关闭整个 Overlay，焦点返回正在播放的视频画面，恢复纯净播放。
2. **自动无操作淡出 (Inactivity Timeout)**：
   * 顶部覆盖层（如主播放控制面板）检测用户无按键输入，**开启 10 秒倒计时定时器**。
   * 任何遥控器按键输入将重置该定时器。
   * 若倒计时到 0，则自动执行淡出动画（Alpha 从 `1.0` 变至 `0.0`），释放焦点锁，恢复全屏纯享。
   * *注意：当 DISC MENU 弹窗处于打开状态时，**不启用**自动淡出，以允许用户长期驻留配置界面。*
