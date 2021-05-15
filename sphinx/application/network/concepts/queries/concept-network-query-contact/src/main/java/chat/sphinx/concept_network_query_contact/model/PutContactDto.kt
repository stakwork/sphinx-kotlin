package chat.sphinx.concept_network_query_contact.model

import com.squareup.moshi.JsonClass

/**
 * Only non-null fields will be serialized to Json for the request body.
 * */
@JsonClass(generateAdapter = true)
data class PutContactDto(
    val route_hint: String? = null,
    val public_key: String? = null,
    val node_alias: String? = null,
    val alias: String? = null,
    val photo_url: String? = null,
    val private_photo: Boolean? = null,
    val contact_key: String? = null,
    val device_id: String? = null,
    val notification_sound: String? = null,
    val tip_amount: Long? = null,
)
