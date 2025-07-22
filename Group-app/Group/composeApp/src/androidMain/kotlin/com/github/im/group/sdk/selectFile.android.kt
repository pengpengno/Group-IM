package com.github.im.group.sdk

actual suspend fun selectFile(): PlatformFile? {
    return object : PlatformFile {
        override val name = "example.txt"
        override val bytes = "Hello from Android!".encodeToByteArray()
    }
}