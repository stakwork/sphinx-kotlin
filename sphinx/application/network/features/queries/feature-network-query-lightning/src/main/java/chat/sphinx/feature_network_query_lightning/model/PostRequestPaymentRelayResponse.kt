package chat.sphinx.feature_network_query_lightning.model

import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_lightning.model.invoice.InvoiceDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import chat.sphinx.wrapper_lightning.RequestPaymentInvoice
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostRequestPaymentRelayResponse(
    override val success: Boolean,
    override val response: RequestPaymentInvoice?,
    override val error: String?
): RelayResponse<RequestPaymentInvoice>()
