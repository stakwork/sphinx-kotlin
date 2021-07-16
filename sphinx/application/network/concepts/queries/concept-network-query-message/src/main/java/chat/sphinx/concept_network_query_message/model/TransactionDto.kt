package chat.sphinx.concept_network_query_message.model

import chat.sphinx.wrapper_common.dashboard.ContactId
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class TransactionDto(
    val id: Long,
    val chat_id: Long?,
    val type: Int,
    val sender: Long,
    val receiver: Long?,
    val amount: Long,
    val payment_hash: String?,
    val payment_request: String?,
    val date: String,
    val reply_uuid: String?,
) {

    @Transient
    var ownerId: ContactId? = null

    fun setOwnerId(value: ContactId) {
        ownerId = value
    }

}