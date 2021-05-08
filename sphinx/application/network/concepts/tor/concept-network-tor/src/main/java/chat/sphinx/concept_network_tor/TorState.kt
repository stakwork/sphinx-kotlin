package chat.sphinx.concept_network_tor

sealed class TorState {
    object On: TorState()
    object Off: TorState()
    object Starting: TorState()
    object Stopping: TorState()
}

sealed class TorNetworkState {
    object Enabled: TorNetworkState()
    object Disabled: TorNetworkState()
}

sealed class TorServiceState {

    abstract val hashCode: Int

    class OnCreate(override val hashCode: Int): TorServiceState()
    class OnDestroy(override val hashCode: Int): TorServiceState()
    class OnBind(override val hashCode: Int): TorServiceState()
    class OnUnbind(override val hashCode: Int): TorServiceState()
    class OnTaskRemoved(override val hashCode: Int): TorServiceState()
}
