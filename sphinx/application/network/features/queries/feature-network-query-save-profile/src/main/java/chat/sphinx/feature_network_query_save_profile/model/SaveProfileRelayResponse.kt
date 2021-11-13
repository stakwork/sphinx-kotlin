package chat.sphinx.feature_network_query_save_profile.model

import chat.sphinx.concept_network_query_save_profile.model.PersonInfoDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SaveProfileRelayResponse(
    override val success: Boolean,
    override val response: PersonInfoDto?,
    override val error: String?
): RelayResponse<PersonInfoDto>()