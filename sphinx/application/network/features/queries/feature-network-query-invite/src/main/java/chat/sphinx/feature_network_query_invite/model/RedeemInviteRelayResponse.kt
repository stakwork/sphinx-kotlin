package chat.sphinx.feature_network_query_invite.model

import chat.sphinx.concept_network_query_invite.model.RedeemInviteResponseDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class RedeemInviteRelayResponse(
    // I marked this as default true because Hub doesn't return a success field but I need one
    // here because it inherits it from RelayResponse
    override val success: Boolean = true,

    @Json(name = "object")
    override val response: RedeemInviteResponseDto?,

    override val error: String?
) :RelayResponse<RedeemInviteResponseDto>()
