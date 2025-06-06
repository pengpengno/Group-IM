package com.github.im.group.model

data class Friend(
    val friendId: Long,
    val name: String,
    val online: Boolean,

    val lastMessage: String? = "" ,
//    val lastMessage: List<String>? = null,
)
