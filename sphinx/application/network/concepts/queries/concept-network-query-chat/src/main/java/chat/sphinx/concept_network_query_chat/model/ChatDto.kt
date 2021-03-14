package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatDto(
    val id: Long,
    val uuid: String,
    val name: String?,
    val photo_url: String?,
    val type: Int,
    val status: Int?,
    val contact_ids: List<Long>,
    val is_muted: Int?,
    val created_at: String,
    val updated_at: String,
    val deleted: Int,
    val group_key: String?,
    val host: String?,
    val price_to_join: Long?,
    val price_per_message: Long?,
    val escrow_millis: Long?,
    val unlisted: Int,
    val private: Int?,
    val owner_pub_key: String?,
    val seen: Int,
    val app_url: String?,
    val feed_url: String?,
    val meta: String?,
    val my_photo_url: String?,
    val my_alias: String?,
    val tenant: Int,
    val skip_broadcast_joins: Int?,
    val pending_contact_ids: List<Long>?
)
