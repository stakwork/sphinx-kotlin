package chat.sphinx.concept_network_query_feed_status

import chat.sphinx.concept_network_query_feed_status.model.ContentFeedStatusDto
import chat.sphinx.concept_network_query_feed_status.model.SyncFeedStatusDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryFeedStatus {

    ////////////
    /// POST ///
    ////////////
    abstract fun sendFeedStatuses(
        syncFeedStatusDto: SyncFeedStatusDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<Any?, ResponseError>>

    ///////////
    /// GET ///
    ///////////
    abstract fun getAllFeedStatuses(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<List<ContentFeedStatusDto>, ResponseError>>
}