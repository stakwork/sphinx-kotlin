package chat.sphinx.feature_network_query_lightning.model

import chat.sphinx.concept_network_query_lightning.model.route.RouteSuccessProbabilityDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckRouteRelayResponse(
    override val success: Boolean,
    override val response: RouteSuccessProbabilityDto?,
    override val error: String?
): RelayResponse<RouteSuccessProbabilityDto>()