# Layouts

## `Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/ui/MainScreen.kt`
Mobile app shell: `ModalNavigationDrawer` + top app bar + Material 3 bottom navigation. Main destinations are Messages, Workbench, Contacts and Profile.

```kotlin
Scaffold(
  topBar = { CenterAlignedTopAppBar(/* menu, title, contextual add action */) },
  bottomBar = { NavigationBar { bottomNavItems.forEach { NavigationBarItem(/*...*/) } } }
) { padding -> /* selected destination */ }
```

## `Group-app/Group-Electronjs/renderer/features/dashboard/Dashboard.tsx`
Desktop shell combines application navigation, conversation list and selected chat room. The AI assistant is supplied as a stable synthetic conversation entry.
