package chat.sphinx.concept_network_query_invite.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HubRedeemInviteResponse(
    @Json(name = "object")
    val response: RedeemInviteResponseDto?,

    val error: String?
)
