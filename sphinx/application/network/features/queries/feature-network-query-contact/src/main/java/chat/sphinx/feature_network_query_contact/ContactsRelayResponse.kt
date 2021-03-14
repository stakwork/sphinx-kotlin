package chat.sphinx.feature_network_query_contact

import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ContactsRelayResponse(
    override val success: Boolean,
    override val response: List<ContactDto>?,
    override val error: String?
) : RelayResponse<List<ContactDto>>()
