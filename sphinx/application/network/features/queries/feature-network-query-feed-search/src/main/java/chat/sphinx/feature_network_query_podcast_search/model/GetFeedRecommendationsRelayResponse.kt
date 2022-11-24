package chat.sphinx.feature_network_query_podcast_search.model

import chat.sphinx.concept_network_query_feed_search.model.FeedRecommendationDto
import chat.sphinx.concept_network_relay_call.RelayListResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetFeedRecommendationsRelayResponse(
    override val success: Boolean,
    override val response: List<FeedRecommendationDto>,
    override val error: String?
) : RelayListResponse<FeedRecommendationDto>()
