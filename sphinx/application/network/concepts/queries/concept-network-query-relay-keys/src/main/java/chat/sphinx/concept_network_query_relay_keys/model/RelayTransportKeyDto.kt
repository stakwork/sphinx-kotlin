package chat.sphinx.concept_network_query_relay_keys.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RelayTransportKeyDto(
    val transport_key: String,
)