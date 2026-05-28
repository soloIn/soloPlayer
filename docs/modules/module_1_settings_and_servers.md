# 模块一：设置与服务器连接管理模块 (Settings & Server Management)

本模块负责 soloPlayer 所有的底层配置、网络服务接入以及凭证的安全管理。重点在于解决 **Emby 服务的可靠连接（自动与手动）、安全鉴权、状态验证** 以及 **局域网 SMB 账户与路径解析的配置管理**。

---

## 1. 业务流程与功能设计

### 1.1 Emby 服务器连接流

```mermaid
graph TD
    A[设置-添加服务页] --> B{选择添加方式}
    B -->|自动发现| C[UDP广播监听]
    B -->|手动添加| D[输入主机地址/端口/HTTPS协议/别名]
    C --> E[展示发现的服务器列表]
    E --> F[点击选择并自动填充地址]
    D --> G[输入用户名与密码鉴权]
    F --> G
    G --> H[点击 "验证并连接"]
    H --> I{鉴权与连接性验证}
    I -->|验证失败| J[界面留在原处 + 显示红色报错卡片]
    I -->|验证成功| K[保存服务配置到数据库]
    K --> L[返回设置主页 + 更新状态为"已连接"并显示详情]
```

### 1.2 核心业务规则
1. **别名设置**：用户可以自定义服务器名称，默认填充为 `"Emby"`。
2. **连接性强验证**：在点击“连接”时，应用**必须**立即发起验证请求。验证通过才能保存，若失败则绝不保存并留在当前编辑页，避免存储脏数据导致系统报错。
3. **不可编辑原则**：为确保本地缓存的元数据与服务器 ID 绝对绑定，已成功添加的 Emby 服务器在列表项中**仅提供“删除”功能，不提供“编辑”功能**。如果服务器信息变更，用户需删除后重新添加。

---

## 2. 界面交互与遥控器对焦链路 (Compose TV UI)

### 2.1 界面组件划分

* **SettingsScreen (设置主页)**：
  * 左侧侧边栏提供垂直分类菜单（播放、音视频、库、服务、通用）。
  * 右侧面板根据左侧选中项进行动态内容加载。在“服务”分类下，展示已绑定的 Emby 服务器卡片及“添加服务器”按钮。
* **ServerAddScreen (服务器添加页 - 独立对话框或全屏页)**：
  * 提供表单项：`别名输入框` -> `IP/域名输入框` -> `端口输入框` -> `HTTPS开关` -> `用户名输入框` -> `密码输入框` -> `测试并保存按钮`。
  * **自动发现面板**：表单下方显示“局域网发现的服务器”列表。

### 2.2 遥控器 D-Pad 导航逻辑

```
  【ServerAddScreen 焦点导航图】
  
  (别名输入框) <───> (IP/域名输入框) <───> (端口/HTTPS开关)
        │
        ▼
  (用户名输入框) <───> (密码输入框)
        │
        ▼
   [测试并保存]  <───> [局域网自动发现卡片1] <───> [卡片2]
```

* **表单焦点流向**：
  * 在别名、IP、端口、用户名、密码输入框之间，按 **Down / Up 键**垂直移动焦点，按 **Left / Right 键**在文本内移动光标。当光标在边缘时按 D-Pad 移动焦点到相邻输入框。
  * 按 **Down 键**移出密码框，焦点落在 `[测试并保存]` 按钮上。
  * 如果验证失败，焦点仍强行锁在 `[测试并保存]` 按钮或出错的输入框上，并在表单最上方弹窗显示错误日志，必须按 **OK 键**关闭报错信息后重新修改输入。

---

## 3. 技术实现方案

### 3.1 UDP 局域网服务发现 (Auto-Discovery)
Emby 服务器在局域网内会通过 **UDP 端口 7359** 进行广播服务。
* **实现机制**：
  * 当用户进入“添加服务器页”时，客户端在协程中启动 UDP Socket 监听。
  * 发送 UDP 广播包到局域网的 `255.255.255.255:7359`，内容为 `who is EmbyServer?`。
  * 接收 Emby 服务端回传的 JSON 广播数据，解析出 `Address`（如 `http://192.168.1.100:8096`）及 `Name`。
  * 使用 `Flow` 实时推送到 Compose 界面列表中展示。

### 3.2 验证性鉴权 API
在保存前，使用 Retrofit 发送鉴权请求：
* **接口**：`POST http://[HOST]:[PORT]/Users/AuthenticateByName`
* **Headers**：
  ```
  X-Emby-Client: soloPlayer TV App
  X-Emby-Device-Name: Android TV Box
  X-Emby-Device-Id: [DEVICE_UUID]
  X-Emby-Client-Version: 1.0.0
  Content-Type: application/json
  ```
* **Body**：
  ```json
  {
    "Username": "[username]",
    "Pw": "[password_sha1_or_raw]"
  }
  ```
* **验证逻辑**：
  * 若返回 `200 OK`：解析响应体中的 `AccessToken` 和 `User.Id`，作为连接合法的唯一凭证，允许执行本地数据库的 `INSERT`。
  * 若返回 `401 Unauthorized` 或发生 `IOException`（连接超时/地址错误）：将 Exception 转化为对用户友好的文案（如“密码错误”或“无法连接到该 IP 地址”），通过 `MutableSharedFlow` 抛给 UI 层展示，阻止页面跳转。

### 3.3 数据持久化 (Room)
定义 `EmbyServerEntity` 实体，存储当前有效绑定的服务信息：

```kotlin
@Entity(tableName = "emby_servers")
data class EmbyServerEntity(
    @PrimaryKey val id: String, // 对应 Emby 返回的 ServerId
    val alias: String,          // 自定义别名，默认 "Emby"
    val serverUrl: String,      // 完整请求基准 URL (http://ip:port)
    val userId: String,         // 当前绑定的 Emby 用户 ID
    val userName: String,       // 用户名
    val accessToken: String,    // 用于 API 鉴权的 Token
    val isConnected: Boolean,   // 状态指示
    val addedAt: Long = System.currentTimeMillis()
)
```

为确保不被修改，其 DAO 仅包含：
```kotlin
@Dao
interface EmbyServerDao {
    @Query("SELECT * FROM emby_servers")
    fun getAllServers(): Flow<List<EmbyServerEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT) // 冲突则拒绝插入，保持状态唯一性
    suspend fun insertServer(server: EmbyServerEntity)

    @Query("DELETE FROM emby_servers WHERE id = :serverId")
    suspend fun deleteServerById(serverId: String)
}
```

---

## 4. SMB 账户与共享配置管理

为了满足“获取 SMB 原生路径进行直连播放”的需求，除了 Emby 服务器连接外，设置模块还需提供 **SMB 账户与授权绑定** 页面。

* **数据实体 `SmbAccountEntity`**：
  ```kotlin
  @Entity(tableName = "smb_accounts")
  data class SmbAccountEntity(
      @PrimaryKey(autoGenerate = true) val id: Int = 0,
      val serverIp: String,      // SMB 服务器 IP（如 192.168.1.100）
      val shareName: String,     // 共享文件夹名（如 Movies）
      val username: String,      // SMB 用户名（匿名访问则为空）
      val password: String       // SMB 密码
  )
  ```
* **直连路径拼接逻辑**：
  当播放器解析到 Emby 返回的物理路径为 `\\192.168.1.100\Movies\BladeRunner.iso` 时：
  1. 客户端通过正则提取 IP `192.168.1.100` 和共享文件夹 `Movies`。
  2. 到 Room 中匹配 `serverIp` 和 `shareName`，查询对应的用户名和密码。
  3. 最终组合出符合 libVLC 识别标准的直连 URL：`smb://username:password@192.168.1.100/Movies/BladeRunner.iso`，传入播放引擎，实现光盘结构的高速直接访问。
