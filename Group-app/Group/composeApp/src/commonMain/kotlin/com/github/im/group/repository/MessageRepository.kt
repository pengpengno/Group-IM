package com.github.im.group.repository

import com.github.im.group.db.AppDatabase

class MessageRepository (
    private val db: AppDatabase
){



    fun addMessage(){
        db.transaction {

        }
    }
}