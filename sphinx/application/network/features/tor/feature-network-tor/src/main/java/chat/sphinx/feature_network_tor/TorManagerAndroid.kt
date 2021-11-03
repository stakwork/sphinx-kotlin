package chat.sphinx.feature_network_tor

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import chat.sphinx.concept_network_tor.SocksProxyAddress
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_network_tor.TorManagerListener
import chat.sphinx.concept_network_tor.TorServiceState
import chat.sphinx.feature_sphinx_service.ApplicationServiceTracker
import chat.sphinx.concept_network_tor.TorState as KTorState
import chat.sphinx.concept_network_tor.TorNetworkState as KTorNetworkState
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.logger.i
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.build_config.BuildConfigVersionCode
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.topl_service.TorServiceController
import io.matthewnelson.topl_service.lifecycle.BackgroundManager
import io.matthewnelson.topl_service.notification.ServiceNotification
import io.matthewnelson.topl_service_base.BaseServiceConsts
import io.matthewnelson.topl_service_base.ServiceExecutionHooks
import io.matthewnelson.topl_service_base.TorPortInfo
import io.matthewnelson.topl_service_base.TorServiceEventBroadcaster
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.system.exitProcess

class TorManagerAndroid(
    application: Application,
    private val applicationScope: CoroutineScope,
    private val authenticationStorage: AuthenticationStorage,
    buildConfigDebug: BuildConfigDebug,
    buildConfigVersionCode: BuildConfigVersionCode,
    private val dispatchers: CoroutineDispatchers,
    private val LOG: SphinxLogger,
) : ApplicationServiceTracker(),
    TorManager,
    CoroutineDispatchers by dispatchers
{

    companion object {
        const val TAG = "TorManagerAndroid"

        // PersistentStorage keys
        const val TOR_MANAGER_REQUIRED = "TOR_MANAGER_REQUIRED"

        @Volatile
        private var isTorRequiredCache: Boolean? = null
        const val TRUE = "T"
        const val FALSE = "F"
    }

    private inner class SphinxBroadcaster: TorServiceEventBroadcaster() {

        @Suppress("RemoveExplicitTypeArguments", "PropertyName")
        val _torServiceStateFlow: MutableStateFlow<TorServiceState> by lazy {
            MutableStateFlow<TorServiceState>(TorServiceState.OnDestroy(-1))
        }

        override fun broadcastServiceLifecycleEvent(event: String, hashCode: Int) {
            when (event) {
                BaseServiceConsts.ServiceLifecycleEvent.CREATED -> {
                    onServiceCreated(mustComplete = true)
                    TorServiceState.OnCreate(hashCode)
                }
                BaseServiceConsts.ServiceLifecycleEvent.DESTROYED -> {
                    onServiceDestroyed(mustComplete = true)
                    TorServiceState.OnDestroy(hashCode)
                }
                BaseServiceConsts.ServiceLifecycleEvent.ON_BIND -> {
                    TorServiceState.OnBind(hashCode)
                }
                BaseServiceConsts.ServiceLifecycleEvent.ON_UNBIND -> {
                    TorServiceState.OnUnbind(hashCode)
                }
                BaseServiceConsts.ServiceLifecycleEvent.TASK_REMOVED -> {
                    onTaskRemoved()
                    TorServiceState.OnTaskRemoved(hashCode)
                }
                else -> {
                    null
                }
            }?.let { state ->
                _torServiceStateFlow.value = state
            }
            LOG.d(TAG, "$event@$hashCode")
        }

        override fun broadcastBandwidth(bytesRead: String, bytesWritten: String) {}

        override fun broadcastDebug(msg: String) {
            LOG.d(TAG, msg)
        }

        override fun broadcastException(msg: String?, e: Exception) {
            LOG.e(TAG, msg ?: "", e)
        }

        override fun broadcastLogMessage(logMessage: String?) {}

        override fun broadcastNotice(msg: String) {
            LOG.i(TAG, msg)
        }

        @Suppress("RemoveExplicitTypeArguments", "PropertyName")
        val _socksProxyAddressStateFlow: MutableStateFlow<SocksProxyAddress?> by lazy {
            MutableStateFlow<SocksProxyAddress?>(null)
        }

        override fun broadcastPortInformation(torPortInfo: TorPortInfo) {
            _socksProxyAddressStateFlow.value = torPortInfo.socksPort?.let { SocksProxyAddress(it) }
        }

        @Suppress("RemoveExplicitTypeArguments", "PropertyName")
        val _torStateFlow: MutableStateFlow<KTorState> by lazy {
            MutableStateFlow<KTorState>(KTorState.Off)
        }

        @Suppress("RemoveExplicitTypeArguments", "PropertyName")
        val _torNetworkStateFlow: MutableStateFlow<KTorNetworkState> by lazy {
            MutableStateFlow<KTorNetworkState>(KTorNetworkState.Disabled)
        }

        override fun broadcastTorState(state: String, networkState: String) {
            _torStateFlow.value = when (state) {
                TorState.ON -> {
                    KTorState.On
                }
                TorState.STARTING -> {
                    KTorState.Starting
                }
                TorState.STOPPING -> {
                    KTorState.Stopping
                }
                else -> {
                    KTorState.Off
                }
            }

            _torNetworkStateFlow.value = if (networkState == TorNetworkState.ENABLED) {
                KTorNetworkState.Enabled
            } else {
                KTorNetworkState.Disabled
            }
        }
    }

    private val broadcaster = SphinxBroadcaster()

    override val socksProxyAddressStateFlow: StateFlow<SocksProxyAddress?>
        get() = broadcaster._socksProxyAddressStateFlow.asStateFlow()

    override suspend fun getSocksPortSetting(): String {
        return withContext(io) {
            TorServiceController.getServiceTorSettings().socksPort
        }
    }

    override val torStateFlow: StateFlow<KTorState>
        get() = broadcaster._torStateFlow.asStateFlow()
    override val torNetworkStateFlow: StateFlow<KTorNetworkState>
        get() = broadcaster._torNetworkStateFlow.asStateFlow()

    override val torServiceStateFlow: StateFlow<TorServiceState>
        get() = broadcaster._torServiceStateFlow.asStateFlow()

    ///////////////////////////////////
    /// Application Service Tracker ///
    ///////////////////////////////////
    override fun onTaskRemoved() {
        super.onTaskRemoved()
        stopTorOrKillApp()
    }

    override fun onServiceDestroyed(mustComplete: Boolean) {
        super.onServiceDestroyed(mustComplete)
        stopTorOrKillApp()
    }

    private fun stopTorOrKillApp() {
        // Tor is running
        if (torServiceStateFlow.value !is TorServiceState.OnDestroy) {
            if (taskIsRemoved && mustCompleteServicesCounter == 1) {
                stopTor()
            }

            // If TorService is _not_ running, and another service that
            // switched to Foreground in onTaskRemoved to complete its task
            // was destroyed while application has been swiped out of recent
            // apps tray has been destroyed
        } else if (taskIsRemoved && mustCompleteServicesCounter == 0) {
            exitProcess(0)
        }
    }

    ///////////////////////////////////////
    /// Tor Service Controller Wrappers ///
    ///////////////////////////////////////
    override fun startTor() {
        TorServiceController.startTor()
    }

    override fun stopTor() {
        TorServiceController.stopTor()
    }

    override fun restartTor() {
        TorServiceController.restartTor()
    }

    override fun newIdentity() {
        TorServiceController.newIdentity()
    }

    /////////////////
    /// Listeners ///
    /////////////////
    private inner class SynchronizedListenerHolder {
        private val listeners: LinkedHashSet<TorManagerListener> = LinkedHashSet(0)

        @Synchronized
        fun addListener(listener: TorManagerListener): Boolean {
            val bool = listeners.add(listener)
            if (bool) {
                LOG.d(TAG, "Listener ${listener.javaClass.simpleName} registered")
            }
            return bool
        }

        @Synchronized
        fun removeListener(listener: TorManagerListener): Boolean {
            val bool = listeners.remove(listener)
            if (bool) {
                LOG.d(TAG, "Listener ${listener.javaClass.simpleName} removed")
            }
            return bool
        }

        @Synchronized
        suspend fun dispatchRequirementChange(required: Boolean) {
            for (listener in listeners) {
                listener.onTorRequirementChange(required)
            }
        }

        @Synchronized
        suspend fun dispatchSocksProxyAddressChange(socksProxyAddress: SocksProxyAddress?) {
            for (listener in listeners) {
                listener.onTorSocksProxyAddressChange(socksProxyAddress)
            }
        }
    }

    private val synchronizedListeners: SynchronizedListenerHolder by lazy {
        SynchronizedListenerHolder()
    }

    override fun addTorManagerListener(listener: TorManagerListener): Boolean {
        return synchronizedListeners.addListener(listener)
    }

    override fun removeTorManagerListener(listener: TorManagerListener): Boolean {
        return synchronizedListeners.removeListener(listener)
    }

    private val lock = Mutex()
    private val requirementChangeLock = Mutex()

    override suspend fun setTorRequired(required: Boolean) {
        lock.withLock {
            val requiredString = if (required) TRUE else FALSE

            when (isTorRequiredCache) {
                null -> {

                    applicationScope.launch(mainImmediate) {
                        requirementChangeLock.withLock {
                            val persisted = authenticationStorage.getString(TOR_MANAGER_REQUIRED, null)

                            if (persisted != requiredString) {
                                authenticationStorage.putString(TOR_MANAGER_REQUIRED, requiredString)
                                isTorRequiredCache = required
                                synchronizedListeners.dispatchRequirementChange(required)
                            } else {
                                isTorRequiredCache = required
                            }
                        }
                    }.join()

                }
                required -> {
                    // no change, do nothing
                }
                else -> {
                    applicationScope.launch(mainImmediate) {
                        requirementChangeLock.withLock {
                            authenticationStorage.putString(TOR_MANAGER_REQUIRED, requiredString)
                            isTorRequiredCache = required
                            synchronizedListeners.dispatchRequirementChange(required)
                        }
                    }.join()
                }
            }
        }
    }

    override suspend fun isTorRequired(): Boolean? {
        lock.withLock {
            requirementChangeLock.withLock {
                return isTorRequiredCache ?: authenticationStorage
                    .getString(TOR_MANAGER_REQUIRED, null).let { persisted ->
                        when (persisted) {
                            null -> {
                                null
                            }
                            TRUE -> {
                                isTorRequiredCache = true
                                true
                            }
                            else -> {
                                isTorRequiredCache = false
                                false
                            }
                        }
                    }
            }
        }
    }

    private inner class SphinxHooks: ServiceExecutionHooks() {
        override suspend fun executeOnCreateTorService(context: Context) {
            socksProxyAddressStateFlow.collect { address ->
                applicationScope.launch(mainImmediate) {
                    synchronizedListeners.dispatchSocksProxyAddressChange(address)
                }.join()
            }
        }
    }

    init {
        TorServiceController.Builder(
            application,

            torServiceNotificationBuilder = ServiceNotification.Builder(
                channelName = TorManager.NOTIFICATION_CHANNEL_NAME,
                channelID = TorManager.NOTIFICATION_CHANNEL_ID,
                channelDescription = TorManager.NOTIFICATION_CHANNEL_DESCRIPTION,
                notificationID = TorManager.NOTIFICATION_ID
            )
//                .setImageTorNetworkingEnabled()
//                .setImageTorNetworkingDisabled()
//                .setImageTorDataTransfer()
//                .setImageTorErrors()
                .setCustomColor(R.color.torPurple)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .enableTorRestartButton(enable = false)
                .enableTorStopButton(enable = false)
                .showNotification(show = true)
                .also { builder ->
                    application.packageManager
                        ?.getLaunchIntentForPackage(application.packageName)
                        ?.let { intent ->
                            builder.setContentIntent(
                                PendingIntent.getActivity(application, 0, intent,  PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            )
                        }
                },

            backgroundManagerPolicy = BackgroundManager.Builder()
                .runServiceInForeground(killAppIfTaskIsRemoved = true),

            buildConfigVersionCode = buildConfigVersionCode.value,
            defaultTorSettings = SphinxTorSettings(),
            geoipAssetPath = "common/geoip",
            geoip6AssetPath = "common/geoip6",
        )

            // ServiceController Builder Options

            .addTimeToDisableNetworkDelay(milliseconds = 4_000)
//            .addTimeToRestartTorDelay()
//            .addTimeToStopServiceDelay()
            .disableStopServiceOnTaskRemoved()
            .setBuildConfigDebug(buildConfigDebug = buildConfigDebug.value)
            .setEventBroadcaster(eventBroadcaster = broadcaster)
            .setServiceExecutionHooks(executionHooks = SphinxHooks())
//            .useCustomTorConfigFiles()
            .build()

        LOG.d(TAG, "TorOnionProxyLibrary-Android initialized")
    }
}
