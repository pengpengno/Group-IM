package com.github.im.group

import okio.FileSystem

actual fun platformFileSystem(): FileSystem {
    return FileSystem.SYSTEM
}