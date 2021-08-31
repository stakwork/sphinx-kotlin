package chat.sphinx.feature_network_query_subscription.model

import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeleteSubscriptionRelayResponse(
    override val success: Boolean,
    override val response: Any?,
    override val error: String?
): RelayResponse<Any>()
