package chat.sphinx.concept_network_tor

import kotlinx.coroutines.flow.StateFlow

interface TorManager {

    companion object {
        // Notification
        const val NOTIFICATION_CHANNEL_NAME = "Tor"
        const val NOTIFICATION_CHANNEL_ID = "Sphinx - Tor"
        const val NOTIFICATION_CHANNEL_DESCRIPTION = "Tor for Sphinx Chat"
        const val NOTIFICATION_ID = 21_615

        const val DEFAULT_SOCKS_PORT = 9250
    }

    val socksProxyAddressStateFlow: StateFlow<SocksProxyAddress?>

    /**
     * Will return the currently set socks port, either the default, or user
     * customized port setting.
     *
     * Note: this could either be `auto`, or an integer
     * */
    suspend fun getSocksPortSetting(): String

    val torStateFlow: StateFlow<TorState>
    val torNetworkStateFlow: StateFlow<TorNetworkState>

    val torServiceStateFlow: StateFlow<TorServiceState>

    fun startTor()
    fun stopTor()
    fun restartTor()
    fun newIdentity()

    /**
     * Caller of this method will also dispatch the change to
     * [TorManagerListener.onTorRequirementChange] for all listeners. If
     * no change is needed, dispatching will not occur.
     * */
    suspend fun setTorRequired(required: Boolean)

    /**
     * t/f if setting has been persisted, otherwise `null`
     * */
    suspend fun isTorRequired(): Boolean?

    /**
     * Any update to the persisted setting via the [setTorRequired]
     * method will dispatch the change to all listeners registered using the caller's
     * scope.
     *
     * @return `true` if the listener was registered, `false` if it was already registered.
     * */
    fun addTorManagerListener(listener: TorManagerListener): Boolean

    /**
     * Removes the listener.
     *
     * @return `true` if the listener was registered and removed, `false` if it wasn't registered
     *  in the first place.
     * */
    fun removeTorManagerListener(listener: TorManagerListener): Boolean
}
