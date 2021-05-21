package chat.sphinx.feature_network_query_message.model

import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageRelayResponse(
    override val success: Boolean,
    override val response: MessageDto?,
    override val error: String?
): RelayResponse<MessageDto>()
