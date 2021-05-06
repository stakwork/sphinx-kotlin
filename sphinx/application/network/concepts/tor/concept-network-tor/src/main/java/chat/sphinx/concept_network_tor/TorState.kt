package chat.sphinx.concept_network_tor

sealed class TorState {
    object On: TorState()
    object Off: TorState()
}

sealed class TorNetworkState {
    object Enabled: TorNetworkState()
    object Disabled: TorNetworkState()
}

sealed class TorServiceState {
    object OnCreate: TorServiceState()
    object OnDestroy: TorServiceState()
    object OnBind: TorServiceState()
    object OnUnbind: TorServiceState()
    object OnTaskRemoved: TorServiceState()
}
