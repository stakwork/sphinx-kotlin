package chat.sphinx.concept_network_query_contact.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostContactDto(
    val alias: String,
    val public_key: String,
    val status: Int,
    val route_hint: String? = null,
    val contact_key: String? = null,
    val photo_url: String? = null,
)
