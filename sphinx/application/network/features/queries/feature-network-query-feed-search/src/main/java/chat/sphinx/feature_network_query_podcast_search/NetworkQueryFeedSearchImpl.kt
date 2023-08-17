package chat.sphinx.feature_network_query_podcast_search

import chat.sphinx.concept_network_query_feed_search.NetworkQueryFeedSearch
import chat.sphinx.concept_network_query_feed_search.model.FeedRecommendationDto
import chat.sphinx.concept_network_query_feed_search.model.FeedSearchResultDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_podcast_search.model.GetFeedRecommendationsRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.isPodcast
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

class NetworkQueryFeedSearchImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryFeedSearch() {

    companion object {
        private const val TRIBES_DEFAULT_SERVER_URL = "https://tribes.sphinx.chat"

        private const val ENDPOINT_PODCAST_SEARCH = "$TRIBES_DEFAULT_SERVER_URL/search_podcasts?q=%s"
        private const val ENDPOINT_YOUTUBE_SEARCH = "$TRIBES_DEFAULT_SERVER_URL/search_youtube?q=%s"
        private const val ENDPOINT_FEED_RECOMMENDATIONS = "/feeds"
    }

    override fun searchFeeds(
        searchTerm: String,
        feedType: FeedType,
    ): Flow<LoadResponse<List<FeedSearchResultDto>, ResponseError>> =
        networkRelayCall.getList(   
            url = String.format(
                if (feedType.isPodcast())
                    ENDPOINT_PODCAST_SEARCH
                else
                    ENDPOINT_YOUTUBE_SEARCH, searchTerm
            ),
            responseJsonClass = FeedSearchResultDto::class.java,
        )

    override fun getFeedRecommendations(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<List<FeedRecommendationDto>, ResponseError>> =
        networkRelayCall.relayGetList(
            responseJsonClass = GetFeedRecommendationsRelayResponse::class.java,
            relayEndpoint = ENDPOINT_FEED_RECOMMENDATIONS,
            relayData = relayData,
            useExtendedNetworkCallClient = true
        )


}