package chat.sphinx.concept_network_client_cache

import chat.sphinx.concept_network_client.NetworkClient
import okhttp3.OkHttpClient

abstract class NetworkClientCache: NetworkClient() {

    companion object {
        const val MAX_STALE_VALUE = 60 * 60 * 24 * 30 // 1 month
    }

    abstract suspend fun getCachingClient(): OkHttpClient
}
