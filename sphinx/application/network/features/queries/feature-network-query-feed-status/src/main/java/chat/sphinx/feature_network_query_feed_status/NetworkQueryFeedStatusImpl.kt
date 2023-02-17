package chat.sphinx.feature_network_query_feed_status

import chat.sphinx.concept_network_query_feed_status.NetworkQueryFeedStatus
import chat.sphinx.concept_network_query_feed_status.model.ContentFeedStatusDto
import chat.sphinx.concept_network_query_feed_status.model.PostFeedStatusDto
import chat.sphinx.concept_network_query_feed_status.model.PutFeedStatusDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_feed_status.model.ContentFeedStatusRelayGetListResponse
import chat.sphinx.feature_network_query_feed_status.model.ContentFeedStatusRelayGetResponse
import chat.sphinx.feature_network_query_feed_status.model.ContentFeedStatusRelayPostResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.feed.FeedId
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
        private const val ENDPOINT_CONTENT_FEED_STATUS_FEED_ID = "/content_feed_status/%s"
    }

    override fun saveFeedStatuses(
        feedStatusesDto: PostFeedStatusDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any?, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = ContentFeedStatusRelayPostResponse::class.java,
            relayEndpoint = ENDPOINT_CONTENT_FEED_STATUS,
            requestBodyJsonClass = PostFeedStatusDto::class.java,
            requestBody = feedStatusesDto,
            relayData = relayData
        )

    override fun saveFeedStatus(
        feedId: FeedId,
        feedStatusDto: PutFeedStatusDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any?, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = ContentFeedStatusRelayPostResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_CONTENT_FEED_STATUS_FEED_ID, feedId.value),
            requestBodyJsonClass = PutFeedStatusDto::class.java,
            requestBody = feedStatusDto,
            relayData = relayData
        )

    override fun getFeedStatuses(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<List<ContentFeedStatusDto>, ResponseError>> =
        networkRelayCall.relayGetList(
            responseJsonClass = ContentFeedStatusRelayGetListResponse::class.java,
            relayEndpoint = ENDPOINT_CONTENT_FEED_STATUS,
            relayData = relayData,
            useExtendedNetworkCallClient = true
        )

    override fun getByFeedId(
        feedId: FeedId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ContentFeedStatusDto, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = ContentFeedStatusRelayGetResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_CONTENT_FEED_STATUS_FEED_ID, feedId.value),
            relayData = relayData,
            useExtendedNetworkCallClient = true
        )
}
