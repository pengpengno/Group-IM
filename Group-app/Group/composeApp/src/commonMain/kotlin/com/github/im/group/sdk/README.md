# SDK Package Structure

This directory contains various SDK modules organized by functionality:

## Packages

### `media` 
- Components for handling media files (images, videos, audio)
- Cross-platform media players and viewers
- File picking and selection utilities
- Media recording functionality (voice, video)
- Android-specific implementations for media handling
- Platform file picker panel implementation

### `webrtc`
- WebRTC implementation for real-time communication
- Video/Audio call management
- Media stream handling
- Peer connection management
- Android-specific WebRTC implementations

### `messaging`
- Message building and sending utilities
- Chat message composition
- Message formatting and validation

### `permission`
- Cross-platform permission handling
- Runtime permission requests
- Permission status management
- Android-specific permission implementations

## Guidelines

- Each package should contain only related functionality
- Common interfaces should be defined in commonMain
- Platform-specific implementations should be in respective directories (androidMain, iosMain, etc.)
- Import statements should reflect the package structure: `com.github.im.group.sdk.<package>`
- UI components (like panels, dialogs) should not be placed in SDK, they belong in UI layers
- Only functional SDK logic (recording, playback, file handling, network, etc.) should be in SDK