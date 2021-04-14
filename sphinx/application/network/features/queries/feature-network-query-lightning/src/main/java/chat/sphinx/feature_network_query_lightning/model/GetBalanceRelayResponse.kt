package chat.sphinx.feature_network_query_lightning.model

import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetBalanceRelayResponse(
    override val success: Boolean,
    override val response: BalanceDto?,
    override val error: String?
): RelayResponse<BalanceDto>()
