package chat.sphinx.feature_network_query_contact.model

import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_contact.model.GenerateTokenResponse
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class GenerateTokenRelayResponse(
    override val success: Boolean,
    override val response: GenerateTokenResponse?,
    override val error: String?
) :RelayResponse<GenerateTokenResponse>()
