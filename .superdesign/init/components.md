# Shared UI components

## `Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/ui/chat/BotCardMessage.kt`
Robot reply card; existing mobile rendering primitive.

```kotlin
@Composable
fun BotCardMessage(card: BotCard, onAction: (BotCardAction) -> Unit) { /* renders title, summary, sections and actions */ }
```

## `Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/ui/chat/ChatBubble.kt`
Shared chat message container, responsible for sender alignment and message content slot.

```kotlin
@Composable
fun ChatBubble(message: ChatMessage, isOwnMessage: Boolean, content: @Composable () -> Unit) { /* existing bubble shell */ }
```

## `Group-app/Group-Electronjs/renderer/features/chat/ChatRoom.tsx`
Existing web robot card renders a title, summary, sections and actions; reuse this content structure for all bot cards.

```tsx
const BotCard = ({ card }: { card: BotCardData }) => (
  <div className="bot-card">
    {card.title && <div className="bot-card-title">{card.title}</div>}
    {card.summary && <div className="bot-card-summary">{card.summary}</div>}
    {card.actions?.map(action => <button className="bot-card-action">{action.label}</button>)}
  </div>
);
```
