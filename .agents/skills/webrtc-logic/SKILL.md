---
name: WebRTC Management & Communication
description: Standards and guidelines for implementing and modifying WebRTC video conferencing features across Server, Mobile (Compose Multiplatform), and Desktop (Electron).
---

# WebRTC Video Conference Logic Standardization

This skill document defines the architecture and communication protocol for the project's WebRTC video conferencing system. All modifications to the video call logic must adhere to these standards to ensure cross-platform compatibility.

## 1. Signaling Protocol & Flow

The signaling server communicates via WebSockets. Both the Caller and Callee exchange state before opening the Peer-to-Peer connection.

### Message Structure
All signaling messages use a unified JSON payload (`WebrtcMessage`):
```json
{
  "type": "...",             // Message type action
  "fromUser": "userId",      // Sender IDs
  "toUser": "userId",        // Target IDs
  "sdp": "...",              // SDP payload for offer/answer
  "sdpType": "offer",        // 'offer' or 'answer'
  "candidate": { ... },      // JSON object of IceCandidateData
  "reason": "..."            // Error or rejection reason
}
```

### Supported Message Types
- `call/request` - Caller asks Callee to start a call.
- `call/accept` - Callee agrees to the call. 
- `call/end` - Either party cancels or hangs up.
- `call/failed` - Connection error.
- `offer` - WebRTC SDP Offer.
- `answer` - WebRTC SDP Answer.
- `candidate` - WebRTC ICE Candidate.

### Standard Connection Flow (Delayed Offer Pattern)
1. **Calling**: Caller sends `call/request`. Wait for response.
2. **Acceptance**: Callee displays UI. If accepted, Callee sends `call/accept`.
3. **Offer Generation**: Caller receives `call/accept`, creates PeerConnection, generates `offer`, and sends to Callee.
4. **Answer Generation**: Callee receives `offer`, creates `answer`, and sends to Caller.
5. **ICE Candidates**: Both parties automatically exchange `candidate` messages sequentially until stream opens.

## 2. API Convergence

All client apps must expose a unified interface (`WebRTCManager`), regardless of platform differences (Android vs Electron).

### Unified `WebRTCManager` API Standard
Ensure that Web, Mobile, and Desktop all contain the following method signatures uniformly:

- `initiateCall(remoteUserId: String)`: Sent by Caller to start the flow.
- `acceptCall(callId: String)`: Sent by Callee when UI accept button is tapped.
- `rejectCall(callId: String)`: Sent by Callee when UI reject button is tapped.
- `endCall()`: Terminates active call, shuts down PeerConnection and stops physical device streams.

### Shared State Management
Ensure that all states use the uniform State Machine approach with these states:
`IDLE` -> `OUTGOING`/`INCOMING` -> `CONNECTING` -> `ACTIVE` -> `ENDING` -> `ENDED`

## 3. Best Practices
- **Resource Management**: Device hardware (cameras, microphones) MUST be released and gracefully shut down immediately upon call termination.
- **Unified Notification**: Any generic exception or failed WebRTC step should leverage the app's unified notification mechanism.
- **WebSocket Reconnection**: Websockets must attempt to reconnect with exp-backoff to handle mobile signal drops, without dropping the background WebRTC session if ICE is successfully maintaining P2P connection.
