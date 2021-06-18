package chat.sphinx.concept_network_query_attachment.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AttachmentAuthenticationTokenDto(
    val token: String,
)
