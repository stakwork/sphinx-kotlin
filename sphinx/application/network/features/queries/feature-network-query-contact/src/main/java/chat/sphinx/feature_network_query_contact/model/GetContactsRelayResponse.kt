package chat.sphinx.feature_network_query_contact.model

import chat.sphinx.concept_network_query_contact.model.GetContactsResponse
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetContactsRelayResponse(
    override val success: Boolean,
    override val response: GetContactsResponse?,
    override val error: String?
) : RelayResponse<GetContactsResponse>()
