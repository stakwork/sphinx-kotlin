package chat.sphinx.feature_network_query_people.model

import chat.sphinx.concept_network_query_people.model.ChatLeaderboardDto
import chat.sphinx.concept_network_relay_call.RelayListResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetLeaderboardRelayResponse(
    override val success: Boolean,
    override val response: List<ChatLeaderboardDto>,
    override val error: String?
) : RelayListResponse<ChatLeaderboardDto>()
