package chat.sphinx.feature_network_query_invite.model

import chat.sphinx.concept_network_query_invite.model.RedeemInviteResponse
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class RedeemInviteRelayResponse(
    override val success: Boolean = true,

    @Json(name = "object")
    override val response: RedeemInviteResponse?,

    override val error: String?
) :RelayResponse<RedeemInviteResponse>()
