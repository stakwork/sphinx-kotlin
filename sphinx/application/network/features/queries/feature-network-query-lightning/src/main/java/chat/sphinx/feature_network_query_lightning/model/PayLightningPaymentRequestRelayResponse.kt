package chat.sphinx.feature_network_query_lightning.model

import chat.sphinx.concept_network_query_lightning.model.invoice.PaymentMessageDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PayLightningPaymentRequestRelayResponse(
    override val success: Boolean,
    override val response: PaymentMessageDto?,
    override val error: String?
): RelayResponse<PaymentMessageDto>()
