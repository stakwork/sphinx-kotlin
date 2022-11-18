package chat.sphinx.feature_network_query_podcast_search.model

import chat.sphinx.concept_network_query_feed_search.model.FeedRecommendationsDto
import chat.sphinx.concept_network_query_feed_search.model.FeedRecommendationsResponse
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetFeedRecommendationsRelayResponse(
    override val success: Boolean,
    override val response: FeedRecommendationsResponse,
    override val error: String?
) : RelayResponse<FeedRecommendationsResponse>()
