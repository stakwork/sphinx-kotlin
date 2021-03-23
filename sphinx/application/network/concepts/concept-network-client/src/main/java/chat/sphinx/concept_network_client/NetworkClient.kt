package chat.sphinx.concept_network_client

import okhttp3.OkHttpClient

abstract class NetworkClient {
    abstract suspend fun getClient(): OkHttpClient
}
