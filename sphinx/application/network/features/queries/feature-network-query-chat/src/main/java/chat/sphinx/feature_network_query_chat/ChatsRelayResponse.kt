package chat.sphinx.feature_network_query_chat

import chat.sphinx.concept_network_query_chat.ChatDto
import chat.sphinx.network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal class ChatsRelayResponse(
    override val success: Boolean,
    override val response: List<ChatDto>?,
    override val error: String?
): RelayResponse<List<ChatDto>>()
