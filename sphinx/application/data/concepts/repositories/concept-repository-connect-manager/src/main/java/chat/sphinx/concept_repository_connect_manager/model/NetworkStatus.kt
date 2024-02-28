package chat.sphinx.concept_repository_connect_manager.model

sealed class NetworkStatus {

    object Loading: NetworkStatus()
    object Connected: NetworkStatus()
    object Disconnected: NetworkStatus()
}