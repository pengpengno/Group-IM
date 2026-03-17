---
name: Cross-Platform Boundary Rules
description: Guidelines for distinguishing and maintaining boundaries between Web, Server, Mobile (Android), and Desktop (Electron) in the Group IM project.
---

# Cross-Platform Boundary Rules

This skill provides essential guidelines for coding and debugging across the `Group` IM ecosystem. The project consists of four distinct platforms: **Server (Java/Kotlin)**, **Web (React/TypeScript)**, **Desktop (Electron/React)**, and **Mobile (Android/Compose)**.

## 1. Build and Output Environments (Electron & Web)

**Key Rule:** Never let concurrent builds clobber each other's outputs.

### Concurrency Issues
- **Avoid Global Cleans in Watch Mode:** When `build:main` and `build:renderer` are both running `--watch` concurrently, using plugins like `CleanWebpackPlugin` on the shared output directory (`dist`) will cause intermittent `ERR_FILE_NOT_FOUND` errors (e.g., `dist/index.html` being deleted every time `main.ts` changes).
- **Distinguish Build Contexts:** Ensure Webpack configurations explicitly differentiate dev vs. production. `clean` should only run on full builds, NOT on incremental `--watch` compilations unless scoped directly to their generated files.

## 2. Platform-Specific Feature Access (Web vs. Electron)

**Key Rule:** Core UI components must degrade gracefully or mock functionality if OS-level features are absent.

### Electron/Desktop Boundaries
- **IPC over Direct Access:** Never access `fs` or native Node.js modules directly from the React renderer components. All system interactions must happen via `ipcRenderer` and be exposed through `preload.ts` (Context Isolation).
- **Web vs. Electron Distinctions:** If sharing code between a pure Web browser environment and an Electron environment, wrap native calls in conditional checks:
  ```typescript
  if (window.electronAPI) {
    window.electronAPI.invokeFeature();
  } else {
    // Fallback Web implementation or mock
  }
  ```

## 3. Server vs. Client Boundary (Java/Spring vs. Frontend)

**Key Rule:** Keep the transport layer abstract; clients shouldn't strictly depend on WebSocket if HTTP offers the same persistence mechanism.

- **Unified Protocols:** When sending messages (e.g., chat), the server now handles both HTTP and TCP (Socket/Proto) via shared base handlers. Frontends must cleanly switch or identify which protocol they are initialized on (e.g., `socketService` vs `httpService`).
- **Domain Mappings:** The Server uses `Long` (or Integer IDs internally) but handles numbers larger than JavaScript's `MAX_SAFE_INTEGER`. Always serialize `ID`s as `String` inside JSON payloads before the Web or Electron client consumes them.

## 4. Mobile (Android/Compose) Boundary Considerations

**Key Rule:** Android operates on independent lifecycle models compared to the Web DOM.

- **Background Limitations:** Unlike Electron or Web, Android handles WebRTC / Sockets differently when the application goes into the background. Rely on Android Foreground Services for continuous IM streams and WebRTC connections.
- **Compose UI vs. React UI:** Layout structures shouldn't strictly attempt 1:1 translation. While CSS properties like `flexbox` apply well to React, Jetpack Compose utilizes `Column`, `Row`, and `Box` with explicit `Modifier` arrangements.

## 5. Debugging & Error Boundaries

- **Electron Renderer:** Check the frontend console via `BrowserWindow.webContents.openDevTools()`. If the window doesn't show up, check `ERR_FILE_NOT_FOUND` on `index.html` loading.
- **Socket / WebRTC:** Use `chrome://webrtc-internals/` or the Electron equivalent to trace ICE candidates. Ensure `onIncomingCall` or similar listeners are mounted *before* signaling triggers.
- **Server:** Always check log output for hibernate / schema validation or network protocol (`Netty`/`HTTP`) decoding mismatches.

By observing these boundaries, cross-platform regressions like disappearing build output files or inaccessible native `window` properties on the web are systematically prevented.
