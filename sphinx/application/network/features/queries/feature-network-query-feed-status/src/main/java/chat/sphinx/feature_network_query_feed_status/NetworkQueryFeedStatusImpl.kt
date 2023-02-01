package chat.sphinx.feature_network_query_feed_status

import chat.sphinx.concept_network_query_feed_status.NetworkQueryFeedStatus
import chat.sphinx.concept_network_query_feed_status.model.ContentFeedStatusDto
import chat.sphinx.concept_network_query_feed_status.model.SyncFeedStatusDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_feed_status.model.ContentFeedStatusRelayGetResponse
import chat.sphinx.feature_network_query_feed_status.model.ContentFeedStatusRelayPostResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

class NetworkQueryFeedStatusImpl(
    private val networkRelayCall: NetworkRelayCall
) : NetworkQueryFeedStatus() {

    companion object {
        private const val ENDPOINT_CONTENT_FEED_STATUS = "/content_feed_status"
    }

    override fun sendFeedStatuses(
        syncFeedStatusDto: SyncFeedStatusDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any?, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = ContentFeedStatusRelayPostResponse::class.java,
            relayEndpoint = ENDPOINT_CONTENT_FEED_STATUS,
            requestBodyJsonClass = SyncFeedStatusDto::class.java,
            requestBody = syncFeedStatusDto,
            relayData = relayData
        )

    override fun getAllFeedStatuses(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<List<ContentFeedStatusDto>, ResponseError>> =
        networkRelayCall.relayGetList(
            responseJsonClass = ContentFeedStatusRelayGetResponse::class.java,
            relayEndpoint = ENDPOINT_CONTENT_FEED_STATUS,
            relayData = relayData,
            useExtendedNetworkCallClient = true
        )
}
