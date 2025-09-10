package com.github.im.group.db

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver

interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver


    fun createDatabase(): AppDatabase
}



// Enum adapter 示例
enum class FileStatus { UPLOADING, SUCCESS, FAILED }
val fileStatusAdapter = object : ColumnAdapter<FileStatus, String> {
    override fun decode(databaseValue: String) = FileStatus.valueOf(databaseValue)
    override fun encode(value: FileStatus) = value.name
}

enum class StorageType { LOCAL, OSS }
val storageTypeAdapter = object : ColumnAdapter<StorageType, String> {
    override fun decode(databaseValue: String) = StorageType.valueOf(databaseValue)
    override fun encode(value: StorageType) = value.name
}