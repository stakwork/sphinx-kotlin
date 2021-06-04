package chat.sphinx.concept_network_query_invite.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InviteDto(
    val id: Long,
    val invite_string: String,
    val invoice: String?,
    val welcome_message: String,
    val contact_id: Long,
    val status: Int,
    val price: Long?,
    val created_at: String,
    val updated_at: String,
)
