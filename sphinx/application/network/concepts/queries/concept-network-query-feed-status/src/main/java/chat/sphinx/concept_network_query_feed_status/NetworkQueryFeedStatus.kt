package chat.sphinx.concept_network_query_feed_status

import chat.sphinx.concept_network_query_feed_status.model.ContentFeedStatusDto
import chat.sphinx.concept_network_query_feed_status.model.PostFeedStatusDto
import chat.sphinx.concept_network_query_feed_status.model.PutFeedStatusDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryFeedStatus {

    ///////////
    /// GET ///
    ///////////
    abstract suspend fun checkYoutubeVideoAvailable(
        videoId: String,
    ): String?

}