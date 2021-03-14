package chat.sphinx.concept_network_query_contact.model

import chat.sphinx.concept_network_query_invite.model.InviteDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ContactDto(
    val id: Long,
    val route_hint: String?,
    val pub_key: String,
    val node_alias: String?,
    val alias: String,
    val photo_url: String?,
    val private_photo: Int?,
    val owner: Int,
    val deleted: Int,
    val auth_token: String?,
    val remote_id: Int?,
    val status: Int?,
    val contact_key: String,
    val device_id: String?,
    val created_at: String,
    val updated_at: String,
    val from_group: Int,
    val notification_sound: String?,
    val last_active: String?,
    val tip_amount: Long?,
    val invite: InviteDto?
)
