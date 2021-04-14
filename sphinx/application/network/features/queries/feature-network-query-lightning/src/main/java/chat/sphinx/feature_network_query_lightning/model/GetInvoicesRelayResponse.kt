package chat.sphinx.feature_network_query_lightning.model

import chat.sphinx.concept_network_query_lightning.model.invoice.InvoiceDto
import chat.sphinx.concept_network_query_lightning.model.invoice.InvoicesDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

/**
 * The endpoint doesn't return a [RelayResponse], yet one is required for the call. This,
 * is simply a work around for Moshi and the call adapter.
 * */
@JsonClass(generateAdapter = true)
data class GetInvoicesRelayResponse(
    val invoices: List<InvoiceDto>,
    val last_index_offset: Long,
    val first_index_offset: Long,

    override val success: Boolean = true,
    override val response: InvoicesDto = InvoicesDto(invoices, last_index_offset, first_index_offset),
    override val error: String?
): RelayResponse<InvoicesDto>()
