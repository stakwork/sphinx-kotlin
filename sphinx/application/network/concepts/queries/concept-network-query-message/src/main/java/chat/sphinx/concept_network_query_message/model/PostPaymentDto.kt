package chat.sphinx.concept_network_query_message.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostPaymentDto(
    val chat_id: Long?,
    val contact_id: Long?,
    val amount: Long,
    val text: String?,
    val remote_text: String?,
    val destination_key: String? = null,
    val route_hint: String? = null,
    val muid: String? = null,
    val media_type: String? = null,
    val dimensions: String? = null,
    val memo: String? = null,
    val remote_memo: String? = null
) {
    init {
        require(!(chat_id == null && contact_id == null && destination_key == null)) {
            "A chat_id and/or contact_id OR a destination_key is required"
        }
    }

    val isKeySendPayment: Boolean
        get() = destination_key != null && destination_key.isNotEmpty()
}