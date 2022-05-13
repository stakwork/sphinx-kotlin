package chat.sphinx.feature_network_query_version

import chat.sphinx.concept_network_query_version.model.AppVersionsDto
import chat.sphinx.concept_network_query_version.NetworkQueryVersion
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_version.model.GetAppVersionsRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

class NetworkQueryVersionImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryVersion() {

    companion object {
        private const val ENDPOINT_APP_VERSIONS = "/app_versions"
    }

    override fun getAppVersions(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<AppVersionsDto, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = GetAppVersionsRelayResponse::class.java,
            relayEndpoint = ENDPOINT_APP_VERSIONS,
            relayData = relayData
        )
}