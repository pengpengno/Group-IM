# Group IM Premium Design System Specification

This document defines the visual language, interaction patterns, and coding standards for the Group IM project to ensure consistency and a premium user experience across all modules.

## 1. Visual Philosophy: "Celestial Glass"
The design follows a **Dark-First, Glassmorphic** approach. It combines deep space backgrounds with vibrant, glowing elements and translucent glass panels.

### Key Characteristics:
- **Depth**: Use of `backdrop-filter: blur()`, subtle shadows, and layered z-index.
- **Vibrancy**: Use of gradients and glowing spheres for background depth.
- **Precision**: High-contrast typography and razor-sharp component borders.

---

## 2. Color Palette

### 2.1 Core Colors
- **App Background**: `#0c111b` (Deep Space Dark Blue)
- **Glass Panel**: `rgba(255, 255, 255, 0.95)` (White with high alpha for readability)
- **Primary Action**: Gradient from `#3b82f6` to `#2563eb` (Royal Blue)
- **Secondary Action**: `rgba(255, 255, 255, 0.1)` (Ghost/Translucent)

### 2.2 Accent Glows (Background Spheres)
- **Focus Blue**: `#3b82f6`
- **Mystic Purple**: `#8b5cf6`
- **Energetic Pink**: `#ec4899`

### 2.3 Typography & Status
- **Headings**: `#111827` (on white cards) or `#FFFFFF` (on dark backgrounds)
- **Body/Secondary**: `#6b7280`
- **Success**: `#10b981`
- **Error**: `#ef4444`
- **Warning**: `#f59e0b`

---

## 3. Component Standards

### 3.1 Containers & Cards
- **Border Radius**: `24px` for main cards, `16px` for sub-containers.
- **Effects**: 
  - `backdrop-filter: blur(20px)`
  - `box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5)`
- **Padding**: Large whitespace (`40px` - `48px` for main cards).

### 3.2 Form Elements
- **Inputs**: 
  - `border-radius: 12px`
  - `border: 2px solid #e5e7eb`
  - Focus state: `border-color: #3b82f6; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1)`
- **Buttons**:
  - `padding: 14px 24px`
  - Hover: `transform: translateY(-2px); box-shadow: (elevation increase)`
  - Active: `transform: translateY(0)`

### 3.3 Feedback (Notifications & Loading)
- **Notifications**: Top-right placement, blurred background, 5px left-accent border based on type.
- **Loading**: Full-screen overlay with `backdrop-filter: blur(8px)` and the "Premium Spinner" (concentric rotating circles).

---

## 4. Interaction & Motion

### 4.1 Easing & Timing
- **Standard Easing**: `cubic-bezier(0.4, 0, 0.2, 1)`
- **Bouncy Easing**: `cubic-bezier(0.18, 0.89, 0.32, 1.28)` (for notifications)

### 4.2 Entry Animations
- **Cards**: Fade in with a `30px` translation from the corresponding direction (left/right/bottom).
- **Background**: Slow-moving `20s` floating animations for gradient spheres.

---

## 5. Coding Implementation Rules
1. **Reuse Components**: Always use `Loading`, `Notification`, and base layout components before writing ad-hoc styles.
2. **CSS Variables**: (Planned) Move hardcoded hex colors to a global CSS variables file.
3. **Accessibility**: All interactive elements must have clear `:focus` states and `aria-label` where applicable.
4. **SVG Icons**: Always use SVG (Lucide-style or Material rounded) for crisp rendering on high-DPI screens.
