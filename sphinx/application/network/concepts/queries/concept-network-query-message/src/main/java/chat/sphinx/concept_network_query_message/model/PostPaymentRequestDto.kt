package chat.sphinx.concept_network_query_message.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostPaymentRequestDto(
    val chat_id: Long?,
    val contact_id: Long?,
    val amount: Long,
    val memo: String? = null,
    val remote_memo: String? = null,
)