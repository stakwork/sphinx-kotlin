package chat.sphinx.feature_network_query_lightning.model

import chat.sphinx.concept_network_query_lightning.model.invoice.LightningPaymentInvoiceDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostInvoicePaymentRelayResponse(
    override val success: Boolean,
    override val response: LightningPaymentInvoiceDto?,
    override val error: String?
): RelayResponse<LightningPaymentInvoiceDto>()
