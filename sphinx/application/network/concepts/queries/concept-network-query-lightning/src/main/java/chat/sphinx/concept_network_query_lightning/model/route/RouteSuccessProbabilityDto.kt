package chat.sphinx.concept_network_query_lightning.model.route

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RouteSuccessProbabilityDto(
    val success_prob: Double
)