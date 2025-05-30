package com.github.im.group

import kotlin.random.Random

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }

    fun greets():List<String> = buildList{
        add(if (Random.nextBoolean()) "Hi!" else "Hello!")

        add("Guess what this is ! > ${platform.name.reversed()}")
    }
}