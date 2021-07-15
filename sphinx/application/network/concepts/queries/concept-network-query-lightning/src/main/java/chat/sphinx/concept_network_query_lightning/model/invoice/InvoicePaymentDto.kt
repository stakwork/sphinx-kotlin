package chat.sphinx.concept_network_query_lightning.model.invoice

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("SpellCheckingInspection")
data class InvoicePaymentDto(
    val invoice: String
)