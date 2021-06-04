package chat.sphinx.concept_network_query_lightning.model.route

import com.squareup.moshi.JsonClass

inline val RouteSuccessProbabilityDto.isRouteAvailable: Boolean
    get() = success_prob > 0

@JsonClass(generateAdapter = true)
data class RouteSuccessProbabilityDto(
    val success_prob: Double
)