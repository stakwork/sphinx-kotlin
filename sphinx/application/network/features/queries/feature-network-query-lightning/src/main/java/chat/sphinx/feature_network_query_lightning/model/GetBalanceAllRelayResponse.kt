package chat.sphinx.feature_network_query_lightning.model

import chat.sphinx.concept_network_query_lightning.model.balance.BalanceAllDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetBalanceAllRelayResponse(
    override val success: Boolean,
    override val response: BalanceAllDto?,
    override val error: String?
): RelayResponse<BalanceAllDto>()
