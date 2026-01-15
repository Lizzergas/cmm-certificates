package com.cmm.certificates

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Helloooo, ${platform.name}!"
    }
}
