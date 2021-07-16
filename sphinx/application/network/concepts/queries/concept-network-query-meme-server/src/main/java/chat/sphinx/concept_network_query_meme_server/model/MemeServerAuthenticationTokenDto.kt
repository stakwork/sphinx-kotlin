package chat.sphinx.concept_network_query_meme_server.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MemeServerAuthenticationTokenDto(
    val token: String,
)
