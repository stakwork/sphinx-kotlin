package chat.sphinx.concept_network_tor

import kotlinx.coroutines.flow.StateFlow

interface TorManager {

    companion object {
        // Notification
        const val NOTIFICATION_CHANNEL_NAME = "Tor"
        const val NOTIFICATION_CHANNEL_ID = "Sphinx - Tor"
        const val NOTIFICATION_CHANNEL_DESCRIPTION = "Tor for Sphinx Chat"
        const val NOTIFICATION_ID = 21_615
    }

    val socksProxyAddressStateFlow: StateFlow<SocksProxyAddress?>

    val torStateFlow: StateFlow<TorState>
    val torNetworkStateFlow: StateFlow<TorNetworkState>

    val torServiceStateFlow: StateFlow<TorServiceState>

    fun startTor()
    fun stopTor()
    fun restartTor()
    fun newIdentity()

}
