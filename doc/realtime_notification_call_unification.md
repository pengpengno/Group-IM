# Group IM Realtime, Notification, and Call Unification Design

## 1. Scope

This document defines a unified design for:

- chat message realtime delivery
- mobile push notification and lock-screen notification
- web notification and interaction behavior
- unified video call and meeting signaling across mobile, web, and desktop

It is based on the current implementation in:

- server IM push: `server/src/main/java/com/github/im/server/service/MessageService.java`
- server websocket signaling: `server/src/main/java/com/github/im/server/handler/SignalWebSocketHandler.java`
- mobile chat sync: `Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/viewmodel/ChatRoomViewModel.kt`
- mobile TCP IM sender: `Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/SenderSdk.kt`
- mobile TCP client: `Group-app/Group/composeApp/src/androidMain/kotlin/com/github/im/group/connect/AndroidSocketClient.kt`
- mobile WebRTC signaling: `Group-app/Group/composeApp/src/androidMain/kotlin/com/github/im/group/sdk/WebRTC.android.kt`
- desktop/web realtime gateway: `Group-app/Group-Electronjs/renderer/services/socketService.ts`
- desktop/web WebRTC manager: `Group-app/Group-Electronjs/renderer/services/WebRTCService.ts`

## 2. Current State Summary

### 2.1 Chat message delivery

Current flow:

1. client sends IM protobuf
2. `ChatProcessServiceHandler` or websocket binary handler calls `MessageService.handleMessage`
3. `MessageService` persists message and pushes `BaseMessagePkg` to conversation members
4. online clients consume realtime package and refresh local state

This works for foreground realtime delivery, but it is not a complete notification system.

### 2.2 Mobile background limitation

Current mobile architecture relies on a foreground TCP long connection:

- `SenderSdk` maintains remote registration
- `AndroidSocketClient` receives binary IM messages and heartbeat
- chat UI binds active conversation handlers through `MessageRouter`

This means:

- Android lock screen or background can break or suspend the TCP channel
- iOS cannot rely on long-lived custom sockets for background message delivery
- background message delivery is not guaranteed by system policy

Conclusion:

Mobile lock-screen message receipt cannot be solved only by fixing the existing socket logic. A system push channel is required.

### 2.3 Web and desktop current state

- Electron and browser now share a mixed realtime path in `socketService`
- browser mode tries native websocket and falls back to polling
- signaling and IM binary websocket share the same `/ws` endpoint on the server
- desktop notification support exists, but notification orchestration is not unified

### 2.4 Video call current state

Current video call is already moving toward room-based mesh meetings:

- signaling center: `SignalWebSocketHandler`
- mobile signaling client: `WebRTC.android.kt`
- web/desktop signaling client: `WebRTCService.ts`

However, the protocol is still divergent:

- some clients still use `call/request` and `call/end`
- other paths use `meeting/request`, `meeting/join`, `meeting/leave`
- message names are inconsistent in a few places
- signaling ownership is duplicated between realtime and WebRTC layers

## 3. Target Principles

### 3.1 Realtime is not notification

Foreground realtime and system notification are separate layers.

- realtime solves low-latency in-app updates
- notification solves wakeup, lock-screen alerting, and re-entry

### 3.2 One event, many transports

The server should produce one canonical event and route it to:

- IM realtime transport
- mobile push transport
- web push transport
- desktop system notification transport

### 3.3 One signaling protocol across all clients

Video call and meeting signaling must use one shared message model and one shared state machine across mobile, web, and desktop.

### 3.4 1v1 call is a 2-person meeting

Keep one room-based protocol and one room lifecycle. Do not maintain separate long-term `call/*` and `meeting/*` business protocols.

## 4. Unified Architecture

### 4.1 Delivery layers

The system should be split into three layers.

#### Layer A: Foreground realtime

Used when app or page is open and active.

- mobile: TCP protobuf IM plus signaling websocket
- web: websocket IM plus signaling
- desktop: Electron IPC TCP bridge plus websocket signaling

Responsibilities:

- new message delivery
- unread updates
- read receipt propagation
- in-call signaling
- active conversation hydration

#### Layer B: Background push

Used when client is backgrounded, locked, suspended, or offline.

- Android: FCM
- iOS: APNs for chat, VoIP push for calls
- browser: Web Push via Service Worker
- Electron desktop: optional local OS notifications when renderer is hidden

Responsibilities:

- wake device or browser context
- display lock-screen notification
- deep-link back into conversation or incoming call UI

#### Layer C: Notification orchestration

A server-side notification policy layer decides:

- whether realtime only is enough
- whether push is required
- whether call invite must be full-screen or ringing
- whether a conversation is muted
- whether notification should collapse or badge

## 5. Server Notification Event Model

### 5.1 Canonical event

Introduce a canonical event model, named here as `ClientEvent`.

Suggested fields:

```json
{
  "eventId": "uuid",
  "eventType": "chat.message.created",
  "transportHint": "realtime|push|both",
  "priority": "normal|high|critical",
  "conversationId": 123,
  "messageId": 456,
  "sequenceId": 789,
  "senderId": 1001,
  "senderName": "alice",
  "senderAvatar": "https://...",
  "receiverId": 1002,
  "roomId": "meeting-123",
  "callId": "meeting-123",
  "title": "Alice",
  "body": "Hello",
  "preview": "Hello",
  "deepLink": "group://chat/123",
  "collapseKey": "chat-123",
  "badgeDelta": 1,
  "ttlSeconds": 86400,
  "extra": {}
}
```

### 5.2 Event types

Recommended event types:

- `chat.message.created`
- `chat.message.mentioned`
- `chat.read.updated`
- `friend.request.created`
- `call.invite.created`
- `call.invite.canceled`
- `call.state.changed`
- `meeting.invite.created`
- `meeting.state.changed`

### 5.3 Client presence classification

The current online/offline model is not enough. Add client activity state:

- `ONLINE_FOREGROUND`
- `ONLINE_BACKGROUND`
- `OFFLINE`
- `IN_CALL`

This can be tracked per device session.

Server notification policy examples:

- `ONLINE_FOREGROUND`: realtime only for normal chat
- `ONLINE_BACKGROUND`: realtime plus push if message not acknowledged in short window
- `OFFLINE`: push directly
- `IN_CALL`: suppress generic chat banners, still allow high-priority call control events

### 5.4 Push endpoint registration contract

Each client should register its push capability explicitly after login or token refresh.

Suggested endpoint API:

- `POST /api/push/endpoints`
- `GET /api/push/endpoints`
- `DELETE /api/push/endpoints/{endpointId}`

Suggested request shape:

```json
{
  "endpointId": null,
  "platform": "ANDROID|IOS|WEB",
  "provider": "FCM|APNS|WEB_PUSH",
  "deviceId": "stable-device-id",
  "token": "fcm-or-apns-token",
  "endpointUrl": "web-push-endpoint",
  "p256dh": "web-push-key",
  "auth": "web-push-auth-secret",
  "locale": "zh-CN",
  "appVersion": "1.0.0",
  "sandbox": false,
  "enabled": true
}
```

Rules:

- Android registers FCM token
- iOS registers APNs token and later VoIP token as a separate high-priority channel if needed
- browser registers Web Push subscription
- client must re-register after token refresh, reinstall, logout/login, or permission reset

## 6. Mobile Push Notification Design

### 6.1 Android

#### Chat notifications

Use FCM data push as primary transport.

Required behavior:

- if app is foreground: do not show system notification by default
- if app is background or locked: show notification through local notification manager
- click notification deep-links to conversation screen
- support group name, sender name, preview, unread count, mute policy

Suggested Android components:

- `FirebaseMessagingService`
- notification channels:
  - `chat_messages`
  - `mentions`
  - `calls`
  - `meetings`
- deep links to conversation and meeting routes

#### Call notifications

Use high-priority FCM data push.

Required behavior:

- show heads-up or full-screen incoming call UI
- wake app process if needed
- allow accept and reject actions directly from notification
- accepting call should reconnect signaling and then join room

#### Foreground service

Use Android foreground service only for active ongoing call or meeting. Do not use it as the main solution for ordinary chat push.

### 6.2 iOS

#### Chat notifications

Use APNs standard remote notifications.

Required behavior:

- app in foreground: in-app banner or local surface
- app in background or locked: APNs alert notification
- clicking notification opens the conversation

#### Call notifications

Use VoIP push plus CallKit.

Required behavior:

- incoming call appears as system call UI
- accepting call launches app and joins the meeting room
- rejection or timeout informs server to clean up invite state

Important note:

iOS background custom websocket or TCP is not a sufficient design for incoming call or lock-screen message alerting.

### 6.3 Mobile local state rules

Push is not the source of truth.

After receiving a push:

1. wake client
2. navigate into target surface if user taps
3. sync server data by conversation or room
4. reconcile local cache and unread status

This avoids trust in push payload completeness.

## 7. Web Notification Design

### 7.1 Three web states

#### State A: page focused

Use in-app interaction only:

- unread badge in chat list
- toast/banner for new conversation
- optional document title flash

Do not show browser system notification by default.

#### State B: tab backgrounded but browser still alive

Use Web Notification API:

- request notification permission
- show sender, conversation name, preview
- clicking notification focuses tab and opens conversation

#### State C: browser closed or no active tab

Use Web Push:

- Service Worker
- Push API
- server subscription storage
- deep-link to conversation or call route

### 7.2 Web call interaction

Incoming call behavior:

- if page active: full-screen incoming call overlay
- if tab backgrounded: high-priority browser notification
- if browser closed and push subscription exists: web push invite with join deep-link

Do not auto-open camera permissions on push receipt. Open permission flow only after explicit user accept.

## 8. Unified Video Call and Meeting Protocol

### 8.1 Canonical protocol

Unify all clients on these signaling message types:

- `meeting/request`
- `meeting/join`
- `meeting/participants`
- `meeting/participant-joined`
- `meeting/participant-left`
- `meeting/reject`
- `meeting/leave`
- `meeting/end`
- `offer`
- `answer`
- `candidate`

Deprecate long-term use of:

- `call/request`
- `call/accept`
- `call/end`
- mismatched aliases such as `participant/joined`

If compatibility is needed during migration, the server may temporarily translate old names to new names, but clients should converge to the canonical set.

### 8.2 Canonical signaling payload

```json
{
  "type": "meeting/request",
  "fromUser": "1001",
  "fromUserName": "alice",
  "fromAvatar": "https://...",
  "toUser": "1002",
  "roomId": "meeting-123",
  "participants": [
    { "userId": "1002", "userName": "bob", "avatar": "..." }
  ],
  "sdp": null,
  "sdpType": null,
  "candidate": null,
  "reason": null
}
```

Candidate shape:

```json
{
  "candidate": {
    "candidate": "candidate:...",
    "sdpMid": "0",
    "sdpMLineIndex": 0
  }
}
```

### 8.3 Unified room lifecycle

#### Outgoing 1v1 call or meeting

1. initiator creates `roomId`
2. initiator sends `meeting/request` to invitees
3. initiator also sends `meeting/join` for self
4. callee accepts and sends `meeting/join`
5. server sends `meeting/participants` to joiner
6. existing participants send `offer` to the newcomer
7. newcomer returns `answer`
8. both sides exchange `candidate`
9. room transitions to active call state

#### Leave and end

- participant exits: `meeting/leave`
- server broadcasts `meeting/participant-left`
- host ends room: `meeting/end`
- clients clean peer state and UI

### 8.4 Room membership rules

Server responsibilities:

- maintain in-memory room membership
- on websocket disconnect, remove user from every room
- broadcast `meeting/participant-left`
- clean empty rooms

### 8.5 Offer creation rule

Keep one rule across all clients:

- newcomer joins room
- existing members create offers to newcomer
- newcomer does not eagerly create offers to all existing members

This matches the current intended behavior and reduces glare.

## 9. Client Responsibility Split

### 9.1 Realtime service

Each platform should have one unified realtime gateway.

Responsibilities:

- connect transport
- expose connection state
- send IM payload
- send signaling payload
- receive IM package
- receive signaling package
- receive notification package

Suggested interface:

- `connect()`
- `disconnect()`
- `sendImPayload()`
- `sendSignalMessage()`
- `markConversationRead()`
- `onImMessage()`
- `onSignalMessage()`
- `onNotificationEvent()`
- `onConnectionStateChange()`

### 9.2 WebRTC manager

WebRTC manager should only own:

- local media
- peer connection map
- remote stream map
- ICE queue
- call state machine

It should not own the transport lifecycle if a shared realtime service already exists.

### 9.3 UI layer

UI should only consume the state machine:

- `IDLE`
- `INCOMING`
- `OUTGOING`
- `CONNECTING`
- `ACTIVE`
- `MINIMIZED`
- `ENDED`
- `ERROR`

## 10. Recommended Cross-Platform API Convergence

### 10.1 Desktop and web

Target direction:

- `socketService` becomes the only realtime entry
- `WebRTCService` consumes signaling from `socketService`
- remove duplicate websocket ownership from `WebRTCService`

### 10.2 Mobile

Target direction:

- keep one IM realtime service for protobuf
- keep one signaling service for text websocket, or formally unify them under one realtime facade
- do not couple chat page lifecycle to notification lifecycle
- add push entry as a parallel transport, not as a fallback inside chat view model

### 10.3 Shared contract files

Introduce shared canonical definitions for:

- signaling type enum
- call state enum
- notification event type enum
- deep-link route contract

The mobile Kotlin and web TypeScript definitions should map 1:1.

## 11. Interaction Design Rules

### 11.1 Chat message interaction

- foreground active conversation: inline append only
- foreground different conversation: unread badge plus toast
- background or locked: system notification
- muted conversation: no sound, optional badge only
- mentioned in group: higher priority channel and visual emphasis

### 11.2 Incoming call interaction

- one active incoming call surface at a time
- if already in a call, new invite gets busy or missed-call response
- 30 second default ringing timeout
- accept, reject, timeout, and cancel must all be represented in protocol and analytics

### 11.3 Meeting interaction

- group chat can initiate meeting from natural entry
- invite shows participant context
- join path should not immediately request camera until user chooses join
- support join muted or audio-only as later enhancement

## 12. Rollout Plan

### Phase 1: Protocol cleanup and design alignment

- define canonical signaling enums in Kotlin and TypeScript
- remove divergent message names from clients
- keep temporary server compatibility aliases if needed
- document realtime vs notification boundary

### Phase 2: Server notification orchestration

- add `ClientEvent` model
- add device endpoint registry for Android, iOS, browser push subscriptions
- add notification policy engine using presence and mute settings
- emit push events in parallel with realtime delivery

### Phase 3: Mobile push implementation

- Android FCM integration
- chat message notification channels
- incoming call full-screen notification flow
- iOS APNs chat notification
- iOS VoIP push plus CallKit for calls

### Phase 4: Web notification implementation

- Notification API in active browser
- Service Worker and Web Push for closed/background browser
- deep-link restore flow

### Phase 5: Video call service convergence

- make one realtime gateway per platform
- move all signaling transport ownership out of UI and into service layer
- make WebRTC managers transport-agnostic

## 13. Immediate Engineering Tasks

Priority A:

- define canonical signaling message names and migrate all clients
- add server event model for chat and call notifications
- register device push tokens and browser push subscriptions

Priority B:

- Android FCM chat and call notifications
- Web Notification API and focus/deep-link handling
- desktop notification orchestration from shared realtime events

Priority C:

- iOS APNs and VoIP push implementation
- Web Push Service Worker
- signaling transport convergence cleanup

## 14. Risks and Non-Goals

### Risks

- old and new signaling names coexisting too long
- duplicate notifications when realtime and push are both active
- call invite race between websocket and push delivery
- iOS call behavior blocked if VoIP push is not implemented

### Non-goals for this phase

- SFU media routing
- recording
- moderation roles
- screen-share mixing
- persistent meeting history
- advanced notification digests

## 15. Decision Summary

- keep realtime and notification as separate layers
- add system push for mobile and web background delivery
- unify video call on room-based `meeting/*` signaling
- treat 1v1 call as a two-person meeting
- converge client API so realtime transport is single-owner per platform
