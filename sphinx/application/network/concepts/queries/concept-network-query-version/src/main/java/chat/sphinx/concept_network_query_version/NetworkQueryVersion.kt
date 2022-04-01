package chat.sphinx.concept_network_query_version

import chat.sphinx.concept_network_query_version.model.AppVersionsDto
import kotlinx.coroutines.flow.Flow
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken

abstract class NetworkQueryVersion {

    ///////////
    /// GET ///
    ///////////
    abstract fun getAppVersions(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<AppVersionsDto, ResponseError>>

}