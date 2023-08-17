package chat.sphinx.feature_network_relay_call

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkRelayErrorResponseDto(
    val success: Boolean,
    val error: String?
)