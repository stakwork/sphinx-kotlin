package chat.sphinx.feature_network_query_transport_key.model

import chat.sphinx.concept_network_query_relay_keys.model.RelayTransportKeyDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetTransportKeyRelayResponse(
    override val success: Boolean,
    override val response: RelayTransportKeyDto?,
    override val error: String?
): RelayResponse<RelayTransportKeyDto>()