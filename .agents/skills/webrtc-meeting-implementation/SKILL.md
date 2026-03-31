---
name: WebRTC Meeting Implementation
description: Use when implementing or modifying Group IM audio/video calling, especially multi-party meetings, signaling rooms, participant state, and cross-platform WebRTC behavior across server and Electron/web clients.
---

# WebRTC Meeting Implementation

Use this skill for Group IM audio/video work when the task involves signaling, room membership, participant state, or media negotiation.

## Defaults

- Treat 1v1 call as a special case of a meeting room.
- Prefer room-based signaling over ad-hoc peer-only signaling.
- Keep desktop/mobile native transport separate from browser-only constraints.
- For current architecture, prefer mesh meetings first.
- If scale or bitrate pressure becomes important, call out SFU as the next step instead of forcing it into an incremental patch.

## Required Mental Model

There are three separate layers:

1. Signaling
   Server `/ws` text messages coordinate meeting lifecycle and SDP/ICE exchange.
2. Media
   Each remote participant gets its own `RTCPeerConnection`.
3. UI state
   Call status, participant roster, local media controls, and stream attachment are managed independently from signaling transport.

Do not collapse these concerns into one reducer or one handler.

## Signaling Rules

For meeting work, prefer these message types:

- `meeting/request`
- `meeting/join`
- `meeting/participants`
- `meeting/participant-joined`
- `meeting/participant-left`
- `meeting/reject`
- `meeting/leave`
- `offer`
- `answer`
- `candidate`

Each signaling payload should carry:

- `type`
- `fromUser`
- `toUser` when point-to-point delivery is needed
- `roomId` for all room-scoped actions
- `fromUserName` / `fromAvatar` when useful for UI hydration

Rules:

- A joiner does not eagerly create offers to everyone already in the room.
- Existing room members create offers to the newly joined participant.
- ICE is always routed peer-to-peer with `toUser`.
- Room membership changes must be broadcast by the server.

## Client Rules

When editing the client:

- Never assume a single `RTCPeerConnection`.
- Store peer connections in a map keyed by remote user id.
- Store remote streams in a map keyed by remote user id.
- Keep local stream shared across all peer connections.
- Sync local tracks into every new peer connection.
- Queue ICE candidates if remote description is not ready yet.

Suggested state shape:

- `roomId`
- `participants[]`
- `remoteUserId` as the first or primary remote participant for legacy UI compatibility
- `isMeeting`
- `callStatus`
- `duration`

## UI Rules

- Reuse the existing call overlay where possible.
- For active meetings, render a simple participant grid before attempting advanced layouts.
- Add meeting entry from natural surfaces first, especially group chat.
- Do not redesign the app theme while implementing meeting logic.

## Server Rules

- Keep IM binary websocket handling untouched while extending text signaling.
- Room membership should be in-memory unless the task explicitly requires persistence.
- On websocket disconnect, remove the user from every joined meeting room.
- Broadcast participant leave events on disconnect cleanup.

## Validation Checklist

After changes, verify:

1. 1v1 call still works.
2. Group chat can start a meeting.
3. A second participant joining triggers offer creation from existing members.
4. Leaving a meeting removes that participant from the grid.
5. Browser HTTPS pages use `wss`.
6. Electron still uses the same shared signaling channel without assuming browser-only APIs.

## File Hotspots

- `server/src/main/java/com/github/im/server/handler/SignalMessage.java`
- `server/src/main/java/com/github/im/server/handler/SignalWebSocketHandler.java`
- `Group-app/Group-Electronjs/renderer/services/WebRTCService.ts`
- `Group-app/Group-Electronjs/renderer/features/video-call/useVideoCall.ts`
- `Group-app/Group-Electronjs/renderer/features/video-call/VideoCallScreen.tsx`
- `Group-app/Group-Electronjs/renderer/features/dashboard/Dashboard.tsx`
- `Group-app/Group-Electronjs/renderer/features/chat/ChatRoom.tsx`

## Non-goals For Incremental Patches

Do not quietly introduce these unless explicitly requested:

- SFU media routing
- recording
- moderation roles
- screen-share mixing
- persistent meeting history
- calendar/scheduling

If one of those becomes necessary, surface it as a separate architecture step.
