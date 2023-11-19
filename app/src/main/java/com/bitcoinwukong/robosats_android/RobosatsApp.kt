package com.bitcoinwukong.robosats_android

import android.app.Application
import com.bitcoinwukong.robosats_android.network.TorManager

class RobosatsApp: Application() {
    lateinit var torManager: TorManager private set

    override fun onCreate() {
        super.onCreate()
        torManager = TorManager(this)
    }
}
