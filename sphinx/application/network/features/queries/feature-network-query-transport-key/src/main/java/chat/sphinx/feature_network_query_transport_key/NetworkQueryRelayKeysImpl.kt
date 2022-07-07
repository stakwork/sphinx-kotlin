package chat.sphinx.feature_network_query_transport_key

import chat.sphinx.concept_network_query_relay_keys.NetworkQueryRelayKeys
import chat.sphinx.concept_network_query_relay_keys.model.PostHMacKeyDto
import chat.sphinx.concept_network_query_relay_keys.model.RelayHMacKeyDto
import chat.sphinx.concept_network_query_relay_keys.model.RelayTransportKeyDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_transport_key.model.AddRelayHMacKeyResponse
import chat.sphinx.feature_network_query_transport_key.model.GetHMacKeyRelayResponse
import chat.sphinx.feature_network_query_transport_key.model.GetTransportKeyRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

class NetworkQueryRelayKeysImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryRelayKeys() {

    companion object {
        private const val ENDPOINT_TRANSPORT_KEY = "/request_transport_key"
        private const val ENDPOINT_H_MAC_KEY = "/hmac_key"
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

    override fun getRelayHMacKey(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<RelayHMacKeyDto, ResponseError>> {
        return networkRelayCall.relayGet(
            responseJsonClass = GetHMacKeyRelayResponse::class.java,
            relayEndpoint = ENDPOINT_H_MAC_KEY,
            relayData = relayData
        )
    }

    override fun addRelayHMacKey(
        addHMacKeyDto: PostHMacKeyDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any?, ResponseError>> {
        return networkRelayCall.relayPost(
            relayEndpoint = ENDPOINT_H_MAC_KEY,
            requestBody = addHMacKeyDto,
            requestBodyJsonClass = PostHMacKeyDto::class.java,
            responseJsonClass = AddRelayHMacKeyResponse::class.java,
            relayData = relayData
        )
    }
}