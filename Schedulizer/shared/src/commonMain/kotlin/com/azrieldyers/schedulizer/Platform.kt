package com.azrieldyers.schedulizer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform