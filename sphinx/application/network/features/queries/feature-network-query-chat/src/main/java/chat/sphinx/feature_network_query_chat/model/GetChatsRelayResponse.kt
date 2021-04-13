package chat.sphinx.feature_network_query_chat.model

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetChatsRelayResponse(
    override val success: Boolean,
    override val response: List<ChatDto>?,
    override val error: String?
): RelayResponse<List<ChatDto>>()
