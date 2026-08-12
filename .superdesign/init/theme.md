# Theme

## Compact token summary

| Token | Value |
| --- | --- |
| Primary | `#3B82F6` → `#2563EB` |
| Dark background | `#0C111B` |
| Main text | `#111827` |
| Secondary text | `#6B7280` |
| Muted text | `#94A3B8` |
| Input border | `#E5E7EB` |
| Focus | `#3B82F6` |

Material 3 is used on mobile. Electron uses component CSS with blue primary actions; preserve this palette and use semantic status colors for success/warning/error.

## Source

```kotlin
object ThemeTokens {
  val BackgroundDark = Color(0xFF0C111B)
  val PrimaryBlue = Color(0xFF3B82F6)
  val PrimaryBlueEnd = Color(0xFF2563EB)
  val TextMain = Color(0xFF111827)
  val TextSecondary = Color(0xFF6B7280)
  val TextMuted = Color(0xFF94A3B8)
  val InputBorder = Color(0xFFE5E7EB)
  val InputFocus = Color(0xFF3B82F6)
}
```
