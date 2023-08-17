package chat.sphinx.concept_network_query_feed_search.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeedRecommendationsResponse(
    val recommendations: List<FeedRecommendationDto>
)