package chat.sphinx.concept_network_query_lightning.model.invoice

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InvoicesDto(
    val invoices: List<InvoiceDto>,
    val last_index_offset: Long,
    val first_index_offset: Long,
)
