package com.github.im.group

import com.github.im.group.db.DatabaseDriverFactory

class JsPlatform : Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

 fun createDatabaseDriverFactory(): DatabaseDriverFactory {
    // Web平台暂时返回一个空实现，后续需要实现基于IndexedDB的数据库驱动
    TODO("Not yet implemented for Web platform")
}