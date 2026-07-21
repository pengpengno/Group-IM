# Setting 统一配置方案

## 1. 背景

当前项目里已经存在多处“设置相关能力”，但入口、存储方式、命名和边界还不统一：

- KMP 端已有 [SettingsUI](/D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/ui/settings/SettingsUI.kt)。
- KMP 端已有本地消息通知偏好接口 [NotificationPreferenceStore](/D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/manager/NotificationPreferenceStore.kt)。
- KMP 端已有本地代理配置存储 [ProxyConfigStorage](/D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/ProxyConfigStorage.kt) 与 [ConfigManager](/D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/config/ConfigManager.kt)。
- Electron 端 `settings` 页目前还是占位，实际内容未落地 [Dashboard.tsx](/D:/ideaproject/pengpeng/Group/Group-app/Group-Electronjs/renderer/features/dashboard/Dashboard.tsx)。
- 服务端已存在用户隐私设置实体 [UserPrivacySetting](/D:/ideaproject/pengpeng/Group/server/src/main/java/com/github/im/server/model/UserPrivacySetting.java)，但还只是单点模型，不是完整“用户设置中心”。

这会导致后续像“视频清晰度”“本地调试地址”“消息通知模式”“隐私开关”“多设备行为”等配置继续分散在各模块里，后面会越来越难维护。

## 2. 目标

把后续所有“可由用户或设备调整的配置”统一收敛到 `Setting` 体系中，并明确：

- 哪些配置是本地配置。
- 哪些配置是账号级远程配置。
- 哪些配置是公司级或服务端策略配置。
- 各端如何以一致的结构读取和更新。

## 3. 核心分层

建议不要再按“页面模块”分，而是按“配置归属”分。

### 3.1 Local Setting

仅对当前设备生效，不需要服务端同步。

适合放这里的内容：

- 代理地址、API Host、TCP Host、端口、TLS。
- 本地缓存策略。
- 本地下载目录、媒体自动保存目录。
- 是否开机自启。
- 是否最小化到托盘。
- 本机通知声音、弹窗样式。
- 调试开关、实验功能开关。
- 本地视频设备偏好。

特点：

- 生命周期属于设备，不属于账号。
- 用户换设备后不自动带过去。
- 应保存在端侧本地存储。

### 3.2 Remote User Setting

对账号生效，需要服务端持久化并可跨设备同步。

适合放这里的内容：

- 视频清晰度偏好。
- 自动接听策略。
- 通知策略。
- 是否显示在线状态。
- 是否允许好友申请。
- 是否显示最后在线时间。
- 消息免打扰范围。
- 通话默认设备策略。
- 会议默认入会行为。

特点：

- 生命周期属于用户账号。
- 同一账号在 Android、iOS、Electron、Web 上应表现一致。
- 应保存在服务端数据库。

### 3.3 Server Policy Setting

不属于用户个人配置，而是服务端或租户级策略。

适合放这里的内容：

- 公司默认视频上限。
- 文件上传大小限制。
- 默认推送策略。
- 是否允许外部联系人。
- 是否允许创建群会议。
- 默认消息保留时长。

特点：

- 由管理员或系统配置。
- 客户端只能读取，不能普通用户直接修改。

## 4. 推荐目录模型

建议把 `Setting` 再按功能域切分，但底层仍然服从 `local / remote / policy` 三层：

- `general`
- `notification`
- `privacy`
- `meeting`
- `video`
- `audio`
- `network`
- `storage`
- `experimental`

其中最关键的是：

- `network` 大概率是本地配置。
- `privacy` 大概率是远程配置。
- `video` 同时包含本地与远程两部分。

## 5. 视频清晰度该怎么放

视频清晰度不建议只做一个字段，建议拆成两个层次。

### 5.1 Remote 偏好

用户账号级默认偏好：

- `LOW`
- `MEDIUM`
- `HIGH`
- `AUTO`

这个值保存在服务端，跨设备同步。

它表达的是：

- 用户默认希望以什么档位发起视频。
- 在没有设备限制时，客户端优先遵循它。

### 5.2 Local 能力覆盖

设备级运行时限制：

- 当前摄像头支持的分辨率。
- 当前网络环境是否差。
- 当前设备性能是否不足。
- 当前是否省电模式。

这个值只在本地决定，不需要写回服务端。

最终实际清晰度应为：

`最终值 = min(远程偏好, 本地能力, 服务端策略)`

这样后续不管是 Electron 还是 Android，都不会把“偏好”和“实际能力”混在一起。

## 6. 推荐数据模型

### 6.1 客户端统一模型

建议客户端统一成三份状态：

```kotlin
data class SettingsBundle(
    val local: LocalSettings,
    val remote: RemoteUserSettings,
    val policy: ServerPolicySettings
)
```

```kotlin
data class LocalSettings(
    val network: NetworkLocalSettings,
    val notification: NotificationLocalSettings,
    val storage: StorageLocalSettings,
    val device: DeviceLocalSettings,
    val experimental: ExperimentalSettings
)

data class RemoteUserSettings(
    val privacy: PrivacySettings,
    val notification: NotificationRemoteSettings,
    val meeting: MeetingSettings,
    val media: MediaSettings
)
```

### 6.2 服务端用户设置模型

当前只有 `UserPrivacySetting` 不够，建议收敛为一个完整的用户设置聚合。

可选方案一：

- 继续拆表。
- `user_privacy_settings`
- `user_notification_settings`
- `user_meeting_settings`
- `user_media_settings`

可选方案二：

- 建一个主表 `user_settings`
- 用结构化 JSON 字段保存各分组

当前项目阶段更建议先用“分组表”或“单表 + 明确字段”，不要一开始就过度 JSON 化，否则后面检索、默认值、迁移都更麻烦。

## 7. 推荐服务端实体方向

建议新增一个聚合实体，而不是继续只靠 `UserPrivacySetting` 顶着。

例如：

```java
public class UserSetting {
    private Long userId;

    // privacy
    private Boolean visibilityStatus;
    private Boolean allowFriendRequest;
    private Boolean showLastOnlineTime;

    // notification
    private String notificationMode;
    private Boolean enableMessagePreview;
    private Boolean enableMeetingInvitePush;

    // media
    private String videoQualityPreference;
    private Boolean autoAdjustVideoQuality;

    // meeting
    private Boolean autoJoinAudio;
    private Boolean autoJoinVideo;
}
```

如果不想立刻替换现有表，也可以先：

1. 保留 `UserPrivacySetting`
2. 新增 `UserMediaSetting`
3. 新增 `UserNotificationSetting`
4. 后面再做聚合 DTO

这是更稳的演进路径。

## 8. 推荐 API 设计

不要为每个开关单独开一个接口，建议按“配置分组”设计。

建议：

- `GET /api/users/me/settings`
- `PUT /api/users/me/settings/privacy`
- `PUT /api/users/me/settings/notification`
- `PUT /api/users/me/settings/media`
- `PUT /api/users/me/settings/meeting`

返回结构建议统一：

```json
{
  "success": true,
  "data": {
    "privacy": {},
    "notification": {},
    "meeting": {},
    "media": {}
  }
}
```

这样各端模型会更稳定，也符合后面 API convergence 的要求。

## 9. 客户端实现建议

### 9.1 KMP 端

建议新增：

- `SettingsRepository`
- `SettingsViewModel`
- `SettingsUiState`

职责：

- `SettingsRepository`
  - 负责拼装本地设置与远程设置
- `SettingsViewModel`
  - 暴露页面状态与更新动作
- `SettingsUiState`
  - 统一给 `SettingsUI` 渲染

`SettingsUI` 不应直接依赖多个 Store 或多个 Manager，否则后续设置项一多会很散。

### 9.2 Electron 端

建议不要继续用目前 `ProfileScreen` 的菜单式占位页作为最终结构，而是改成真正的 Setting 模块：

- `renderer/features/settings/SettingsScreen.tsx`
- `renderer/features/settings/settingsSlice.ts`
- `renderer/features/settings/settingsApi.ts`

页面结构与 KMP 保持一致：

- 通知
- 隐私
- 视频与会议
- 网络与代理
- 存储与设备
- 关于

### 9.3 Web / Electron 边界

按当前项目边界规则：

- Renderer 不直接碰本机文件系统。
- Electron 特有本地配置经 `preload.ts` + IPC 暴露。
- Web 环境则走浏览器可用能力或降级。

因此本地配置能力建议抽象成：

- `LocalSettingsGateway`

由不同平台各自实现。

## 10. 页面结构建议

Setting 首页建议直接分组，不要做成“个人资料页 + 零散入口”。

建议一级分组：

1. 通知
2. 隐私与安全
3. 视频与会议
4. 网络与代理
5. 存储与设备
6. 关于与更新

建议优先第一批落地的设置项：

1. 消息通知开关
2. 消息预览开关
3. 通知模式
4. 视频清晰度偏好
5. 自动音频入会
6. 自动视频入会
7. 在线状态可见性
8. 是否允许好友申请
9. 代理地址
10. TLS 开关

## 11. 推荐落地顺序

### Phase 1

先把模型和页面骨架统一：

- 建立 `SettingsRepository / ViewModel / UiState`
- KMP `SettingsUI` 改为统一配置入口
- Electron `settings` 页从占位改为正式页面
- 把现有通知设置和代理设置收编进去

### Phase 2

补服务端账号级设置：

- 新增用户设置 DTO / Service / Controller
- 完成 `GET / PUT settings` API
- 打通隐私、通知、视频清晰度

### Phase 3

补视频/会议真实能力联动：

- 默认视频清晰度偏好
- 弱网自动降级
- 设备能力覆盖
- 服务端策略上限控制

## 12. 本次整理结论

后续 `Setting` 不建议再按“某个功能页顺手放一点设置”继续扩散，而应该统一遵循下面规则：

- 本机设备相关配置放 `LocalSettings`
- 用户账号跨端同步配置放 `RemoteUserSettings`
- 公司级或系统级限制放 `ServerPolicySettings`
- 视频清晰度属于“远程偏好 + 本地能力 + 服务端策略”共同决策
- 客户端页面统一叫 `Settings`
- 服务端接口统一走 `/api/users/me/settings/*`

## 13. 下一步建议

下一步可以直接开始实装以下最小闭环：

1. KMP 建 `SettingsRepository` 和 `SettingsViewModel`
2. 整理 `SettingsUI`，把通知设置和代理设置合并进来
3. Electron 新建正式 `SettingsScreen`
4. 服务端新增 `UserSettings` 读取与更新接口
5. 第一批先打通 `notification + privacy + videoQualityPreference`

这样做完后，后面再加任何配置项，都不会再散到各个模块里。
