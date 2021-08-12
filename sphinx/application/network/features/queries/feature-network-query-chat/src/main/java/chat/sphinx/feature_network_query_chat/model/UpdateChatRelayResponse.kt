package chat.sphinx.feature_network_query_chat.model

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class UpdateChatRelayResponse(
    override val success: Boolean,
    override val response: ChatDto,
    override val error: String?
): RelayResponse<ChatDto>()