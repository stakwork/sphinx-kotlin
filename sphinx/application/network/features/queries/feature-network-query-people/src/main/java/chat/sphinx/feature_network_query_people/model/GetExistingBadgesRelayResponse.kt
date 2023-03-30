package chat.sphinx.feature_network_query_people.model

import chat.sphinx.concept_network_query_people.model.BadgeDto
import chat.sphinx.concept_network_relay_call.RelayListResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetExistingBadgesRelayResponse(
    override val success: Boolean,
    override val response: List<BadgeDto>,
    override val error: String?
) : RelayListResponse<BadgeDto>()
