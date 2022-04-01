package chat.sphinx.feature_network_query_transport_key

import chat.sphinx.concept_network_query_relay_keys.NetworkQueryRelayKeys
import chat.sphinx.concept_network_query_relay_keys.model.RelayTransportKeyDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_transport_key.model.GetTransportKeyRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQueryRelayKeysImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryRelayKeys() {

    companion object {
        private const val ENDPOINT_TRANSPORT_KEY = "/request_transport_key"
    }

    override fun getRelayTransportKey(
        relayUrl: RelayUrl
    ): Flow<LoadResponse<RelayTransportKeyDto, ResponseError>> {
        return networkRelayCall.relayUnauthenticatedGet(
            responseJsonClass = GetTransportKeyRelayResponse::class.java,
            relayEndpoint = ENDPOINT_TRANSPORT_KEY,
            relayUrl = relayUrl
        )
    }
}