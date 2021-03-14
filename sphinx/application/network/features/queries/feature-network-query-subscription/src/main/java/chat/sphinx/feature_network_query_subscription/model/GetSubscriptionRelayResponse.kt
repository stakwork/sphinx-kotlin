package chat.sphinx.feature_network_query_subscription.model

import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetSubscriptionRelayResponse(
    override val success: Boolean,
    override val response: SubscriptionDto?,
    override val error: String?
): RelayResponse<SubscriptionDto>()
