package chat.sphinx.feature_network_query_verify_external.model

import chat.sphinx.concept_network_query_verify_external.model.VerifyExternalDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerifyExternalRelayResponse(
    override val success: Boolean,
    override val response: VerifyExternalDto?,
    override val error: String?
): RelayResponse<VerifyExternalDto>()