package chat.sphinx.concept_network_query_invite.model

import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HubRedeemInviteResponse(
    override val success: Boolean = true,

    @Json(name = "object")
    override val response: RedeemInviteResponse?,

    override val error: String?
): RelayResponse<RedeemInviteResponse>()
