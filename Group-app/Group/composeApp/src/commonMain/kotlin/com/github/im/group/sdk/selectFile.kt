package com.github.im.group.sdk

expect suspend fun selectFile(): PlatformFile?

interface PlatformFile {
    val name: String
    val bytes: ByteArray
}
