package chat.sphinx.feature_network_query_chat.model

import chat.sphinx.concept_network_query_chat.model.PutPinMessageDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class PinMessageRelayResponse(
    override val success: Boolean,
    override val response: PutPinMessageDto?,
    override val error: String?
): RelayResponse<PutPinMessageDto>()