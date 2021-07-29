package chat.sphinx.feature_network_query_message.model

import chat.sphinx.concept_network_query_message.model.PutMemberResponseDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PutMemberRelayResponse(
    override val success: Boolean,
    override val response: PutMemberResponseDto?,
    override val error: String?
): RelayResponse<PutMemberResponseDto>()
