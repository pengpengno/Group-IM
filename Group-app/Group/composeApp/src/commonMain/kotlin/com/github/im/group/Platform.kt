package com.github.im.group

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform