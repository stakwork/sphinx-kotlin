package chat.sphinx.concept_network_query_attachment.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AttachmentAuthenticationDto(
    val id: String,
    val challenge: String,
)
