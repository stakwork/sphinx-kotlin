package chat.sphinx.concept_network_query_version

import chat.sphinx.concept_network_query_version.model.AppVersionsDto
import kotlinx.coroutines.flow.Flow
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl

abstract class NetworkQueryVersion {

    ///////////
    /// GET ///
    ///////////
    abstract fun getAppVersions(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<AppVersionsDto, ResponseError>>

}