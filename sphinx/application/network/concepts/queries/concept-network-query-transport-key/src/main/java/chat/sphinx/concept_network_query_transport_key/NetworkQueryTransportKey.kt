package chat.sphinx.concept_network_query_transport_key

import chat.sphinx.concept_network_query_transport_key.model.RelayTransportKeyDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryTransportKey {

    ///////////
    /// GET ///
    ///////////
    abstract fun getRelayTransportKey(
        relayUrl: RelayUrl
    ): Flow<LoadResponse<RelayTransportKeyDto, ResponseError>>
}