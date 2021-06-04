package chat.sphinx.feature_network_query_subscription.model

import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetSubscriptionsRelayResponse(
    override val success: Boolean,
    override val response: List<SubscriptionDto>?,
    override val error: String?
): RelayResponse<List<SubscriptionDto>>()
