package com.yakup.bluescreen

import android.app.Application

class BluescreenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // AdMob'u uygulama başlangıcında başlat
        AdManager.init(this)
    }
}
