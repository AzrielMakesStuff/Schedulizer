package com.ateam.schedulizer

import android.app.Application

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        ContextHolder.init(this)
    }
}