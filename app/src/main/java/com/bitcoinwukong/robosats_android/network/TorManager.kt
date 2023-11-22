package com.bitcoinwukong.robosats_android.network

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.matthewnelson.kmp.tor.KmpTorLoaderAndroid
import io.matthewnelson.kmp.tor.TorConfigProviderAndroid
import io.matthewnelson.kmp.tor.common.address.PortProxy
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig
import io.matthewnelson.kmp.tor.controller.common.events.TorEvent
import io.matthewnelson.kmp.tor.manager.TorManager
import io.matthewnelson.kmp.tor.manager.TorServiceConfig
import io.matthewnelson.kmp.tor.manager.common.event.TorManagerEvent
import io.matthewnelson.kmp.tor.manager.common.state.TorNetworkState
import io.matthewnelson.kmp.tor.manager.common.state.TorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class TorManager(private val application: Application)  : ITorManager {
    override fun addLine(line: String) {
        CoroutineScope(Dispatchers.Main).launch {
            listener.addLine(line)
        }
    }

    override fun addListener(listener: TorManagerEvent.SealedListener): Boolean {
        return manager.addListener(listener)
    }

    override fun removeListener(listener: TorManagerEvent.SealedListener): Boolean {
        return manager.removeListener(listener)
    }

    override val addressInfo: TorManagerEvent.AddressInfo
        get() = manager.addressInfo
    override val networkState: TorNetworkState
        get() = manager.networkState
    override val state: TorState
        get() = manager.state

    override fun start() {
        manager.startQuietly()
    }

    override fun restart() {
        manager.restartQuietly()
    }

    private val providerAndroid: TorConfigProviderAndroid = object : TorConfigProviderAndroid(application) {
        override fun provide(): TorConfig {
            return TorConfig.Builder {
                // Set multiple ports for all of the things
                val dns = TorConfig.Setting.Ports.Dns()
                put(dns.set(TorConfig.Option.AorDorPort.Value(PortProxy(9252))))
                put(dns.set(TorConfig.Option.AorDorPort.Value(PortProxy(9253))))

                val socks = TorConfig.Setting.Ports.Socks()
                put(socks.set(TorConfig.Option.AorDorPort.Value(PortProxy(9254))))
                put(socks.set(TorConfig.Option.AorDorPort.Value(PortProxy(9255))))

                val http = TorConfig.Setting.Ports.HttpTunnel()
                put(http.set(TorConfig.Option.AorDorPort.Value(PortProxy(9258))))
                put(http.set(TorConfig.Option.AorDorPort.Value(PortProxy(9259))))

                val trans = TorConfig.Setting.Ports.Trans()
                put(trans.set(TorConfig.Option.AorDorPort.Value(PortProxy(9262))))
                put(trans.set(TorConfig.Option.AorDorPort.Value(PortProxy(9263))))

                // If a port (9263) is already taken (by ^^^^ trans port above)
                // this will take its place and "overwrite" the trans port entry
                // because port 9263 is taken.
                put(socks.set(TorConfig.Option.AorDorPort.Value(PortProxy(9263))))

                // Set Flags
                socks.setFlags(setOf(
                    TorConfig.Setting.Ports.Socks.Flag.OnionTrafficOnly
                )).setIsolationFlags(setOf(
                    TorConfig.Setting.Ports.IsolationFlag.IsolateClientAddr,
                )).set(TorConfig.Option.AorDorPort.Value(PortProxy(9264)))
                put(socks)

                // reset our socks object to defaults
                socks.setDefault()

                // Use a UnixSocket instead of TCP for the ControlPort.
                //
                // A unix domain socket will always be preferred on Android
                // if neither Ports.Control or UnixSockets.Control are provided.
                put(TorConfig.Setting.UnixSockets.Control().set(TorConfig.Option.FileSystemFile(
                    workDir.builder {

                        // Put the file in the "data" directory
                        // so that we avoid any directory permission
                        // issues.
                        //
                        // Note that DataDirectory is automatically added
                        // for you if it is not present in your provided
                        // config. If you set a custom Path for it, you
                        // should use it here.
                        addSegment(TorConfig.Setting.DataDirectory.DEFAULT_NAME)

                        addSegment(TorConfig.Setting.UnixSockets.Control.DEFAULT_NAME)
                    }
                )))

                // Use a UnixSocket instead of TCP for the SocksPort.
                put(TorConfig.Setting.UnixSockets.Socks().set(TorConfig.Option.FileSystemFile(
                    workDir.builder {

                        // Put the file in the "data" directory
                        // so that we avoid any directory permission
                        // issues.
                        //
                        // Note that DataDirectory is automatically added
                        // for you if it is not present in your provided
                        // config. If you set a custom Path for it, you
                        // should use it here.
                        addSegment(TorConfig.Setting.DataDirectory.DEFAULT_NAME)

                        addSegment(TorConfig.Setting.UnixSockets.Socks.DEFAULT_NAME)
                    }
                )))

                // For Android, disabling & reducing connection padding is
                // advisable to minimize mobile data usage.
                put(TorConfig.Setting.ConnectionPadding().set(TorConfig.Option.AorTorF.False))
                put(TorConfig.Setting.ConnectionPaddingReduced().set(TorConfig.Option.TorF.True))

                // Tor default is 24h. Reducing to 10 min helps mitigate
                // unnecessary mobile data usage.
                put(TorConfig.Setting.DormantClientTimeout().set(TorConfig.Option.Time.Minutes(10)))

                // Tor defaults this setting to false which would mean if
                // Tor goes dormant, the next time it is started it will still
                // be in the dormant state and will not bootstrap until being
                // set to "active". This ensures that if it is a fresh start,
                // dormancy will be cancelled automatically.
                put(TorConfig.Setting.DormantCanceledByStartup().set(TorConfig.Option.TorF.True))

                // If planning to use v3 Client Authentication in a persistent
                // manner (where private keys are saved to disk via the "Persist"
                // flag), this is needed to be set.
                put(TorConfig.Setting.ClientOnionAuthDir().set(TorConfig.Option.FileSystemDir(
                    workDir.builder { addSegment(TorConfig.Setting.ClientOnionAuthDir.DEFAULT_NAME) }
                )))
            }.build()
        }
    }

    private val loaderAndroid: KmpTorLoaderAndroid = KmpTorLoaderAndroid(provider = providerAndroid)
    private val manager: TorManager = TorManager.newInstance(application = application, loader = loaderAndroid, requiredEvents = null)
    private val listener = TorListener()
    override val events: LiveData<String> get() = listener.eventLines

    init {
        manager.debug(true)
        manager.addListener(listener)

        listener.addLine(TorServiceConfig.getMetaData(application).toString())
        start()
    }

    private val appScope by lazy {
        CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    }

    private inner class TorListener : TorManagerEvent.Listener() {
        private val _eventLines: MutableLiveData<String> = MutableLiveData("")
        val eventLines: LiveData<String> = _eventLines
        private val events: MutableList<String> = ArrayList(50)

        fun addLine(line: String) {
            synchronized(this) {
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val timestampedLine = "$timestamp: $line"

                if (events.size > 49) {
                    events.removeAt(0)
                }
                events.add(timestampedLine)
                Log.d("TorListener", line)
                _eventLines.postValue(events.joinToString("\n"))
            }
        }

        override fun onEvent(event: TorManagerEvent) {
            addLine(event.toString())

            super.onEvent(event)
        }

        override fun onEvent(event: TorEvent.Type.SingleLineEvent, output: String) {
            if (event.toString() != "BW") {
                addLine("$event - $output")
            }
            super.onEvent(event, output)
        }

        override fun onEvent(event: TorEvent.Type.MultiLineEvent, output: List<String>) {
            addLine("multi-line event: $event. See Logs.")

            // these events are many many many lines and should be moved
            // off the main thread if ever needed to be dealt with.
            appScope.launch(Dispatchers.IO) {
                Log.d("TorListener", "-------------- multi-line event START: $event --------------")
                for (line in output) {
                    Log.d("TorListener", line)
                }
                Log.d("TorListener", "--------------- multi-line event END: $event ---------------")
            }

            super.onEvent(event, output)
        }

        override fun managerEventError(t: Throwable) {
            t.printStackTrace()
        }

        override fun managerEventAddressInfo(info: TorManagerEvent.AddressInfo) {
            if (info.isNull) {
                // Tear down HttpClient
            } else {
                info.socksInfoToProxyAddressOrNull()?.firstOrNull()?.let { proxyAddress ->
                    @Suppress("UNUSED_VARIABLE")
                    val proxy = InetSocketAddress(proxyAddress.address.value, proxyAddress.port.value)

                    // Build HttpClient
                }
            }
        }
    }
}


