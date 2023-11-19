package com.bitcoinwukong.robosats_android.network

import androidx.lifecycle.LiveData
import io.matthewnelson.kmp.tor.controller.common.events.TorEventProcessor
import io.matthewnelson.kmp.tor.manager.common.event.TorManagerEvent
import io.matthewnelson.kmp.tor.manager.common.state.TorStateManager

interface ITorManager : TorEventProcessor<TorManagerEvent.SealedListener>, TorStateManager {
    val events: LiveData<String>

    fun start()
    fun restart()

    fun addLine(line: String)
}