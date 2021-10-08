package chat.sphinx.concept_network_query_meme_server.model

import com.squareup.moshi.*

@JsonClass(generateAdapter = true)
data class PaymentTemplateDto(
    val muid: String,
    val width: Int,
    val height: Int,
)