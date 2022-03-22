package chat.sphinx.feature_network_query_transport_key

import chat.sphinx.concept_network_query_transport_key.NetworkQueryTransportKey
import chat.sphinx.concept_network_query_transport_key.model.RelayTransportKeyDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_transport_key.model.GetTransportKeyRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQueryTransportKeyImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryTransportKey() {

    companion object {
        private const val ENDPOINT_TRANSPORT_KEY = "/request_transport_token"
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