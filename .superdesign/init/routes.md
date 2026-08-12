# Routes

## Mobile (Compose Navigation)

- `ChatMainScreen`: Messages, Workbench, Contacts, Profile tabs.
- `ChatUI` / `ChatRoomScreen`: selected conversation, including AI assistant.
- `Meetings`, `Settings`, `Search`, `Login`: routed from `Routing.kt`.

## Web / Electron

- `Dashboard.tsx`: application shell and conversation selection.
- `ChatList.tsx`: conversation list and fixed `AI 助手` entry.
- `ChatRoom.tsx`: conversation interaction, bot cards, group bot configuration modal.
