package chat.sphinx.concept_network_query_contact.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostContactDto(
    val alias: String,
    val public_key: String,
    val route_hint: String?,
)
