package chat.sphinx.feature_network_query_lightning.model

import chat.sphinx.concept_network_query_lightning.model.webview.PayLsatDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PayLsatRelayResponse(
    override val success: Boolean,
    override val response: PayLsatDto?,
    override val error: String?
): RelayResponse<PayLsatDto>()