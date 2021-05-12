package chat.sphinx.feature_network_query_contact.model

import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_contact.model.CreateContactResponse
import chat.sphinx.concept_network_query_contact.model.GetContactsResponse
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateContactRelayResponse(
    override val success: Boolean = true,
    override val response: ContactDto,
    override val error: String?
): RelayResponse<ContactDto>()