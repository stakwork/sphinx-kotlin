package chat.sphinx.feature_network_query_contact.model

import chat.sphinx.concept_network_query_contact.model.GetContactsResponse
import chat.sphinx.concept_network_query_contact.model.GetLatestContactsResponse
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GetLatestContactsRelayResponse(
    override val success: Boolean,
    override val response: GetLatestContactsResponse?,
    override val error: String?
) : RelayResponse<GetLatestContactsResponse>()
