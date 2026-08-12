# Extractable components

## AppNavigation
- Source: `Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/ui/MainScreen.kt`
- Category: layout
- Description: Mobile top app bar, drawer and bottom navigation.
- Extractable props: `selectedDestination`, `unreadCount`, `onNavigate`.

## ConversationListItem
- Source: `Group-app/Group-Electronjs/renderer/features/chat/ChatList.tsx`
- Category: basic
- Description: Avatar, unread badge, presence and last-message preview.
- Extractable props: `active`, `unreadCount`, `presence`, `isBot`.

## BotCard
- Source: `Group-app/Group-Electronjs/renderer/features/chat/ChatRoom.tsx`
- Category: basic
- Description: Structured AI reply with actions.
- Extractable props: `title`, `summary`, `sections`, `actions`, `status`.
