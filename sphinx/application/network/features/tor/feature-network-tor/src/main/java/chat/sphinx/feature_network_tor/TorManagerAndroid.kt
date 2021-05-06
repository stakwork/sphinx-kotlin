package chat.sphinx.feature_network_tor

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import chat.sphinx.concept_network_tor.SocksProxyAddress
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_network_tor.TorServiceState
import chat.sphinx.concept_network_tor.TorState as KTorState
import chat.sphinx.concept_network_tor.TorNetworkState as KTorNetworkState
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.logger.i
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.build_config.BuildConfigVersionCode
import io.matthewnelson.topl_service.TorServiceController
import io.matthewnelson.topl_service.lifecycle.BackgroundManager
import io.matthewnelson.topl_service.notification.ServiceNotification
import io.matthewnelson.topl_service_base.BaseServiceConsts
import io.matthewnelson.topl_service_base.TorPortInfo
import io.matthewnelson.topl_service_base.TorServiceEventBroadcaster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TorManagerAndroid(
    application: Application,
    buildConfigDebug: BuildConfigDebug,
    buildConfigVersionCode: BuildConfigVersionCode,
    private val LOG: SphinxLogger,
): TorServiceEventBroadcaster(), TorManager {

    companion object {
        const val TAG = "TorManagerAndroid"
    }

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

    init {
        TorServiceController.Builder(
            application,

            // Notification Builder
            ServiceNotification.Builder(
                channelName = TorManager.NOTIFICATION_CHANNEL_NAME,
                channelID = TorManager.NOTIFICATION_CHANNEL_ID,
                channelDescription = TorManager.NOTIFICATION_CHANNEL_DESCRIPTION,
                notificationID = TorManager.NOTIFICATION_ID
            )
//                .setImageTorNetworkingEnabled()
//                .setImageTorNetworkingDisabled()
//                .setImageTorDataTransfer()
//                .setImageTorErrors()
//                .setCustomColor()
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .enableTorRestartButton(enable = false)
                .enableTorStopButton(enable = false)
                .showNotification(show = true)
                .also { builder ->
                    application.packageManager
                        ?.getLaunchIntentForPackage(application.packageName)
                        ?.let { intent ->
                            builder.setContentIntent(
                                PendingIntent.getActivity(application, 0, intent, 0)
                            )
                        }
                },

            // BackgroundManager Policy Builder
            BackgroundManager.Builder()
                .runServiceInForeground(killAppIfTaskIsRemoved = false),

            buildConfigVersionCode = buildConfigVersionCode.value,
            SphinxTorSettings(),
            "common/geoip",
            "common/geoip6",
        )

            // ServiceController Builder Options
//            .addTimeToDisableNetworkDelay()
//            .addTimeToRestartTorDelay()
//            .addTimeToStopServiceDelay()
//            .disableStopServiceOnTaskRemoved()
            .setBuildConfigDebug(buildConfigDebug = buildConfigDebug.value)
            .setEventBroadcaster(this)
//            .setServiceExecutionHooks()
//            .useCustomTorConfigFiles()
            .build()

        LOG.d(TAG, "TorOnionProxyLibrary-Android initialized")
    }

    @Suppress("RemoveExplicitTypeArguments")
    private val _torServiceStateFlow: MutableStateFlow<TorServiceState> by lazy {
        MutableStateFlow<TorServiceState>(TorServiceState.OnDestroy)
    }
    override val torServiceStateFlow: StateFlow<TorServiceState>
        get() = _torServiceStateFlow.asStateFlow()

    override fun broadcastServiceLifecycleEvent(event: String, hashCode: Int) {
        when (event) {
            BaseServiceConsts.ServiceLifecycleEvent.CREATED -> {
                TorServiceState.OnCreate
            }
            BaseServiceConsts.ServiceLifecycleEvent.DESTROYED -> {
                TorServiceState.OnDestroy
            }
            BaseServiceConsts.ServiceLifecycleEvent.ON_BIND -> {
                TorServiceState.OnBind
            }
            BaseServiceConsts.ServiceLifecycleEvent.ON_UNBIND -> {
                TorServiceState.OnUnbind
            }
            BaseServiceConsts.ServiceLifecycleEvent.TASK_REMOVED -> {
                TorServiceState.OnTaskRemoved
            }
            else -> {
                null
            }
        }?.let { state ->
            _torServiceStateFlow.value = state
        }
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

    @Suppress("RemoveExplicitTypeArguments")
    private val _socksProxyAddressStateFlow: MutableStateFlow<SocksProxyAddress?> by lazy {
        MutableStateFlow<SocksProxyAddress?>(null)
    }
    override val socksProxyAddressStateFlow: StateFlow<SocksProxyAddress?>
        get() = _socksProxyAddressStateFlow.asStateFlow()

    override fun broadcastPortInformation(torPortInfo: TorPortInfo) {
        _socksProxyAddressStateFlow.value = torPortInfo.socksPort?.let { SocksProxyAddress(it) }
    }

    @Suppress("RemoveExplicitTypeArguments")
    private val _torStateFlow: MutableStateFlow<KTorState> by lazy {
        MutableStateFlow<KTorState>(KTorState.Off)
    }
    override val torStateFlow: StateFlow<KTorState>
        get() = _torStateFlow.asStateFlow()

    @Suppress("RemoveExplicitTypeArguments")
    private val _torNetworkStateFlow: MutableStateFlow<KTorNetworkState> by lazy {
        MutableStateFlow<KTorNetworkState>(KTorNetworkState.Disabled)
    }
    override val torNetworkStateFlow: StateFlow<KTorNetworkState>
        get() = _torNetworkStateFlow.asStateFlow()

    override fun broadcastTorState(state: String, networkState: String) {
        _torStateFlow.value = if (state == TorState.ON) {
            KTorState.On
        } else {
            KTorState.Off
        }

        _torNetworkStateFlow.value = if (networkState == TorNetworkState.ENABLED) {
            KTorNetworkState.Enabled
        } else {
            KTorNetworkState.Disabled
        }
    }
}
