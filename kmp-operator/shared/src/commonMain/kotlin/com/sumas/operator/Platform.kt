package com.sumas.operator

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform