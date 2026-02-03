package com.github.im.group

import com.github.im.group.db.DatabaseDriverFactory

class JsPlatform : Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

fun createDatabaseDriverFactory(): DatabaseDriverFactory {
    return JsDatabaseDriverFactory()
}