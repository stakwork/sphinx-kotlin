package chat.sphinx.feature_network_relay_call

import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkRelayCallErrorDto(
    override val success: Boolean,
    override val response: Any?,
    override val error: String?
): RelayResponse<Any>()