---
name: API Convergence Rules
description: Guidelines and checks for unifying duplicate APIs and maintaining standard consistency.
---

# API Convergence Skill

As the project supports multiple platforms (Electron for Desktop, Kotlin Multiplatform for Mobile, Spring/Netty for Server), code fragmentation can occur. This skill dictates how to identify, converge, and clean up duplicate APIs.

## 1. What is API Convergence?
API convergence is the practice of unifying the parameter interfaces, method behaviors, state machines, and endpoint URIs so that logic operates consistently. If `WebRTCManager` requires `String callId` on mobile but requires `String remoteUserId` on desktop, the API is divergent and must be converged.

## 2. Server Cleanup Rules
Refactor and eliminate dual-routing:
- E.g., duplicate WebSocket configurations (like `WebSocketServer.java` mapping Netty and `WebSocketConfig.java` mapping Spring).
- **Rule**: If a system heavily relies on `SignalWebSocketHandler` on `/ws`, remove arbitrary prototype endpoints like `/websocket` in Netty configurations to prevent developer confusion. Only ONE definitive source of truth per protocol exists.

## 3. Client Convergence Rules
When modifying an interface in Kotlin (`WebRTC.kt`), simultaneously update the analog mapped interface in TypeScript (`webrtc.ts`):
- Names of functions must map 1:1.
- Arguments must map sequentially and logically. 
- Example: 
  ```kotlin
  // Kotlin
  fun rejectCall(callId: String)
  ```
  ```typescript
  // TypeScript
  rejectCall(callId: string, remoteUserId?: string)
  ```

## 4. Constant Unification
String literals like `"call/request"` should never remain floating strings on clients. Ensure definition into an Enum or union type so that a mismatch compiler error occurs if refactored.

Always apply the above practices during any new feature request!
