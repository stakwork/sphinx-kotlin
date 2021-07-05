package chat.sphinx.concept_network_query_invite.model

import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PayInviteResponse(
    override val success: Boolean,

    @Json(name = "invite")
    override val response: InviteDto,

    override val error: String? = null
): RelayResponse<InviteDto>()