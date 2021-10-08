package chat.sphinx.concept_network_query_message.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PutPaymentRequestDto(
    val payment_request: String
)