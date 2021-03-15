package chat.sphinx.concept_network_query_message.model

import chat.sphinx.concept_network_query_contact.model.ContactDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageDto(
    val id: Long,
    val uuid: String?,
    val chat_id: Long?,
    val type: Int,
    val sender: Long,
    val receiver: Int?,
    val amount: Long,
    val amount_msat: Long,
    val payment_hash: String?,
    val payment_request: String?,
    val date: String,
    val expiration_date: String?,
    val message_content: String?,
    val remote_message_content: String?,
    val status: Int?,
    val status_map: Map<Long, Long>?, // contact_id : their message's 'status'
    val parent_id: Long?,
    val subscription_id: Long?,
    val mediaKey: String?,
    val mediaType: String?,
    val mediaToken: String?,
    val seen: Boolean,
    val created_at: String,
    val updated_at: String,
    val sender_alias: String?,
    val sender_pic: String?,
    val original_muid: String?,
    val reply_uuid: String?,
    val network_type: Int?,
    val tenant: Int,
    val chat: MsgsChatDto?,
    val contact: ContactDto?,
)
