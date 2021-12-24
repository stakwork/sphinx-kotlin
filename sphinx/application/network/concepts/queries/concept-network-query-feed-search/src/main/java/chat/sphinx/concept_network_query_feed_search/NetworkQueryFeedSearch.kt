package chat.sphinx.concept_network_query_feed_search

import chat.sphinx.concept_network_query_feed_search.model.FeedSearchResultDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryFeedSearch {

    ///////////
    /// GET ///
    ///////////
    abstract fun searchFeeds(
        searchTerm: String,
        feedType: FeedType,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<List<FeedSearchResultDto>, ResponseError>>
}