package chat.sphinx.feature_network_query_contact.model

import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class CreateInviteRelayResponse(
    override val success: Boolean,

    @Json(name = "contact")
    override val response: ContactDto?,

    override val error: String? = null
) :RelayResponse<ContactDto>()
