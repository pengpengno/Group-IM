---
trigger: always_on
---

# Group IM Agent Instruction Rules

These rules are for the AI agent (Antigravity) to ensure quality and consistency in code contributions.

## Design Protocol
1. **Reference Design System**: Before implementing any UI changes, refer to [design-system.md](./design-system.md).
2. **Premium First**: Always prioritize high-fidelity, polished UI over simple MVPs.
3. **Consistency**: Ensure new features use existing components (Loading, Notification) to maintain a unified user experience.

## Technical Protocol
1. **Error Handling**: Use the unified notification system for all user-facing errors.
2. **Loading States**: Always provide visual feedback during async operations.
3. **Clean Code**: Follow the project's multi-platform structure (Electron for desktop, Kotlin for mobile).
