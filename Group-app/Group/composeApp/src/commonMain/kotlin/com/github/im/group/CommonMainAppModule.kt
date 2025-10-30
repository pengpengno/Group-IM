package com.github.im.group

import com.github.im.group.db.DatabaseDriverFactory
import com.github.im.group.repository.FilesRepository
import com.github.im.group.sdk.FileStorageManager
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.dsl.module

val commonModule = module {
    single { FilesRepository(get()) }
    single { 
        FileStorageManager(
            filesRepository = get(),
            fileSystem = FileSystem.SYSTEM,
            baseDirectory = Path.Companion.DIRECTORY_SEPARATOR.toPath() // 默认根路径，具体平台会覆盖
        ) 
    }
}