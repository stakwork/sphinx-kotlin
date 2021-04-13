package chat.sphinx.feature_network_query_message.model

import chat.sphinx.concept_network_query_message.model.GetMessagesResponse
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetMessagesRelayResponse(
    override val success: Boolean,
    override val response: GetMessagesResponse?,
    override val error: String?
) : RelayResponse<GetMessagesResponse>()
