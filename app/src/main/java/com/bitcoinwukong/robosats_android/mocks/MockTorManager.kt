package com.bitcoinwukong.robosats_android.mocks

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.bitcoinwukong.robosats_android.network.ITorManager
import io.matthewnelson.kmp.tor.manager.common.event.TorManagerEvent
import io.matthewnelson.kmp.tor.manager.common.state.TorNetworkState
import io.matthewnelson.kmp.tor.manager.common.state.TorState

class MockTorManager: ITorManager {
    override val addressInfo: TorManagerEvent.AddressInfo = TorManagerEvent.AddressInfo.NULL_VALUES
    override val networkState: TorNetworkState = TorNetworkState.Enabled
    override val state: TorState = TorState.Off

    override val events = MutableLiveData<String>()
    override fun start() {
        Log.d("MockTorManager", "start called")
    }

    override fun restart() {
        Log.d("MockTorManager", "restart called")
    }

    override fun addLine(line: String) {
        Log.d("MockTorManager", "addLine called")
    }

    override fun addListener(listener: TorManagerEvent.SealedListener): Boolean {
        Log.d("MockTorManager", "addListener called")
        return true
    }

    override fun removeListener(listener: TorManagerEvent.SealedListener): Boolean {
        Log.d("MockTorManager", "removeListener called")
        return true
    }
}
