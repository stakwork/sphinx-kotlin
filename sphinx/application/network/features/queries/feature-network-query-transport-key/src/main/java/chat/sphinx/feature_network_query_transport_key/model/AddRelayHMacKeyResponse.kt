package chat.sphinx.feature_network_query_transport_key.model

import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class AddRelayHMacKeyResponse(
    override val success: Boolean = true,
    override val response: Any?,
    override val error: String?
): RelayResponse<Any>() 
