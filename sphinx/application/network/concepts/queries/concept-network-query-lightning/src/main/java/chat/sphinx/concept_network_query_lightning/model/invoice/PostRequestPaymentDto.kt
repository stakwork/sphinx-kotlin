package chat.sphinx.concept_network_query_lightning.model.invoice

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostRequestPaymentDto(
    val chat_id: Long?,
    val contact_id: Long?,
    val amount: Long,
    val memo: String? = null,
) {
}