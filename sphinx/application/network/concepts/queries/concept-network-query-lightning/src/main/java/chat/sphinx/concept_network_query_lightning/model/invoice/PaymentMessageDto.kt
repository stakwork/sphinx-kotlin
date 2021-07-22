package chat.sphinx.concept_network_query_lightning.model.invoice

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentMessageDto(
    val success: Boolean,
    val response: PayRequestDto
    )
