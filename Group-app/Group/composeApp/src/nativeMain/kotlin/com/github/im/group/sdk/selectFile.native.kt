package com.github.im.group.sdk

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.getcwd
import platform.posix.getpwuid
import platform.posix.pwd
import platform.posix.uid
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUserDirectory
import platform.posix.getcwd
import platform.Foundation.NSString
import platform.Foundation.NSHomeDirectory
import platform.Foundation.fileSystemRepresentation
import platform.posix.stat
import platform.posix.open
import platform.posix.read
import platform.posix.close

actual suspend fun selectFile(): PlatformFile? {
    // 原生平台的文件选择器实现（占位符）
    TODO("Native platform file picker not implemented")
}

actual suspend fun readBytesFromPath(path: String): ByteArray? {
    // 原生平台的文件读取实现（占位符）
    TODO("Native platform file reading not implemented")
}