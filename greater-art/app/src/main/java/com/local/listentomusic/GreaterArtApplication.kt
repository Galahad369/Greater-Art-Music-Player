package com.local.listentomusic

import android.app.Application

class GreaterArtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.local.listentomusic.diagnostics.CrashReports.install(this)
        com.local.listentomusic.ui.components.VideoAmbientColors.start()
    }
}
