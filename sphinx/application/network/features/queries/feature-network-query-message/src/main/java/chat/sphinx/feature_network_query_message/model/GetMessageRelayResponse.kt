package chat.sphinx.feature_network_query_message.model

import chat.sphinx.concept_network_query_message.model.GetMessageDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetMessageRelayResponse(
    override val success: Boolean,
    override val response: GetMessageDto,
    override val error: String?
): RelayResponse<GetMessageDto>()
