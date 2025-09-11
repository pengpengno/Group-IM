package com.github.im.group.db

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.github.im.group.db.entities.FileStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver


    fun createDatabase(): AppDatabase
}

// 时间日期转化器
val localDateTimeAdapter = object : ColumnAdapter<LocalDateTime, Long> {
    override fun decode(databaseValue: Long): LocalDateTime {
        return Instant.fromEpochMilliseconds(databaseValue).toLocalDateTime(TimeZone.currentSystemDefault())
    }
    override fun encode(value: LocalDateTime): Long {
        return value.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
}


val fileStatusAdapter = object : ColumnAdapter<FileStatus, String> {
    override fun decode(databaseValue: String) = FileStatus.valueOf(databaseValue)
    override fun encode(value: FileStatus) = value.name
}

