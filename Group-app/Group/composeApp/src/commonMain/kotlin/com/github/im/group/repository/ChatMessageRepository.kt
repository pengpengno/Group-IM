package com.github.im.group.repository

import com.github.im.group.db.entities.ChatMessage
import com.github.im.group.db.DatabaseDriverFactory
import db.AppDatabase

class ChatMessageRepository (databaseDriverFactory: DatabaseDriverFactory ){
//    private val database = Database(databaseDriverFactory)



}

internal class ChatMessageDatabase(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = AppDatabase(databaseDriverFactory.createDriver())
    private val dbQuery = database.launchQueries

    internal fun getAllLaunches(): List<ChatMessage>{
        dbQuery.insertMessage(
            msgId = 1,
            fromAccountId = 1,
            conversationId = "1",
            content = "1",
            type = "1",
            status = "1",
            fileResource = null,
            time = "1",
            seqId = 1
        )
        return dbQuery.selectAllLaunchesInfo(::mapLaunchSelecting).executeAsList()

        return dbQ
    }
}