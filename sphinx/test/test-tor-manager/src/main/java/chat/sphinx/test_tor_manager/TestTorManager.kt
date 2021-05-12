package chat.sphinx.test_tor_manager

import chat.sphinx.concept_network_tor.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TestTorManager: TorManager {
    override val socksProxyAddressStateFlow: StateFlow<SocksProxyAddress?>
        get() = MutableStateFlow(null)

    override suspend fun getSocksPortSetting(): String {
        return TorManager.DEFAULT_SOCKS_PORT.toString()
    }

    override val torStateFlow: StateFlow<TorState>
        get() = MutableStateFlow(TorState.Off)
    override val torNetworkStateFlow: StateFlow<TorNetworkState>
        get() = MutableStateFlow(TorNetworkState.Disabled)
    override val torServiceStateFlow: StateFlow<TorServiceState>
        get() = MutableStateFlow(TorServiceState.OnDestroy(-1))

    override fun startTor() {}
    override fun stopTor() {}
    override fun restartTor() {}
    override fun newIdentity() {}

    private var torIsRequired: Boolean? = null
    override suspend fun setTorRequired(required: Boolean) {
        torIsRequired = required
    }

    override suspend fun isTorRequired(): Boolean? {
        return torIsRequired
    }

    override fun addTorManagerListener(listener: TorManagerListener): Boolean {
        return false
    }

    override fun removeTorManagerListener(listener: TorManagerListener): Boolean {
        return false
    }
}