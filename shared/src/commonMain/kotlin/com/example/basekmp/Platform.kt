package com.example.basekmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform