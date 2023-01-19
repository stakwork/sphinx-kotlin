package chat.sphinx.feature_network_query_discover_tribes

import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.concept_network_query_discover_tribes.NetworkQueryDiscoverTribes
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import kotlinx.coroutines.flow.Flow

class NetworkQueryDiscoverTribesImpl(
    private val networkRelayCall: NetworkRelayCall
    ): NetworkQueryDiscoverTribes() {

    companion object {
        private const val TRIBES_DEFAULT_SERVER_URL = "https://tribes.sphinx.chat"

        private const val ENDPOINT_ALL_TRIBES = "$TRIBES_DEFAULT_SERVER_URL/tribes"

    }

    override fun getAllDiscoverTribes(): Flow<LoadResponse<List<TribeDto>, ResponseError>> =
        networkRelayCall.getList(
            url = ENDPOINT_ALL_TRIBES,
            responseJsonClass = TribeDto::class.java
        )
}