# Group IM — AI & automation UI system

Use the established Group IM visual language: clear blue primary action (`#3B82F6`), white/surface backgrounds, dark `#111827` primary text, and restrained elevation. Use Material 3 semantics on mobile and the existing Electron conversation-list/chat-room structure on desktop.

## Product and UX principles

- AI stays inside conversation: no separate “AI product” visual language.
- Automation is transparent: show trigger, scope, next action, execution state and actor.
- Any sensitive action must appear as a proposal card with explicit Approve / Decline; never make implicit execution look like a normal reply.
- Prefer progressive disclosure: chat users see actions in context; administrators get rules, history and controls.

## Tokens

- Primary: `#3B82F6`; pressed: `#2563EB`; focus ring: `#3B82F6`.
- Surface: white; canvas: `#F8FAFC`; border: `#E5E7EB`.
- Text: `#111827`; secondary: `#6B7280`; muted: `#94A3B8`.
- Status: success / warning / danger must use semantic colors plus an icon and text label.
- 8px spacing rhythm; 12px controls; 16px cards; 24px page sections.
- Mobile actions must be at least 44dp. Web desktop must retain keyboard focus visibility and 4.5:1 text contrast.

## Responsive information architecture

- Mobile: bottom navigation → Messages / Workbench / Contacts / Me. Robot directory opens from Messages; automation administration is in Workbench.
- Tablet: message list and detail can coexist; automation editor is a two-step full screen flow.
- Web desktop: three-column workspace when ≥1280px; use a right inspector only for rule details and execution history.
