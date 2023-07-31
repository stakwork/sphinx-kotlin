package chat.sphinx.feature_network_query_feed_status

import chat.sphinx.concept_network_query_feed_status.NetworkQueryFeedStatus
import chat.sphinx.concept_network_query_feed_status.model.ContentFeedStatusDto
import chat.sphinx.concept_network_query_feed_status.model.PostFeedStatusDto
import chat.sphinx.concept_network_query_feed_status.model.PutFeedStatusDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_feed_status.model.ContentFeedStatusRelayGetListResponse
import chat.sphinx.feature_network_query_feed_status.model.ContentFeedStatusRelayGetResponse
import chat.sphinx.feature_network_query_feed_status.model.ContentFeedStatusRelayPostResponse
import chat.sphinx.feature_network_query_feed_status.model.YoutubeVideoRelayGetResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.sign

class NetworkQueryFeedStatusImpl(
    private val networkRelayCall: NetworkRelayCall
) : NetworkQueryFeedStatus() {

    companion object {
        private const val ENDPOINT_CONTENT_FEED_STATUS = "/content_feed_status"
        private const val ENDPOINT_CONTENT_FEED_STATUS_FEED_ID = "/content_feed_status/%s"

        private const val ENDPOINT_YOUTUBE_VIDEO = "https://stakwork-uploads.s3.amazonaws.com/uploads/customers/6040/media_to_local/00002e82-6911-4aea-a214-62c9d88740e0/%s.mp4"

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

    override suspend fun checkYoutubeVideoAvailable(
        videoId: String,
    ): String? {
        var resultUrl: String? = null
        val formattedUrl = String.format(ENDPOINT_YOUTUBE_VIDEO, videoId)

        networkRelayCall.get(
            url = formattedUrl,
            responseJsonClass = Any::class.java
        ).collect { response ->
            if (response is Response.Error && isStatusCode200(response.exception.toString())) {
                resultUrl = formattedUrl
            }
        }

        return resultUrl
    }

    private fun isStatusCode200(response: String): Boolean {
        val codePattern = Regex("code=(\\d+),")
        val matchResult = codePattern.find(response)
        val code = matchResult?.groups?.get(1)?.value
        return "200" == code
    }

}
