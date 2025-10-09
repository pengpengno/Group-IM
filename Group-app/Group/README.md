This is a Kotlin Multiplatform project targeting Android, iOS, Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

是IM 系统的的客户端实现
目标android desktop
How to build project:


1. generate protobuf
```shell
gradle generateProtos
gradle generateSqlDelight
gradle generateCommonMainAppDatabaseInterface
```
2. build apk
Android
```shell
gradle composeApp:assembleDebug
```


3. adb connect 
```shell

adb pair {IP:PORT}

Enter pair code

adb connect {IP}:{PORT}
```
and the apk  would  output on path composeApp/build/outputs/apk/debug/composeApp-debug.apk

https://square.github.io/wire/wire_compiler/#customizing-output