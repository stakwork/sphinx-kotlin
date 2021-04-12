package chat.sphinx.concept_network_client_cache

import chat.sphinx.concept_network_client.NetworkClient
import okhttp3.OkHttpClient

abstract class NetworkClientCache: NetworkClient() {
    abstract suspend fun getCachingClient(): OkHttpClient
    abstract fun addOnClientClearedCallback(onClear: () -> Unit)
    abstract fun removeOnClientClearedCallback()
}
