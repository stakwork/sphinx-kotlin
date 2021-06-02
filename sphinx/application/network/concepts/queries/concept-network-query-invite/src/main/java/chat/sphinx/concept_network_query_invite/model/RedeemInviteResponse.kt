package chat.sphinx.concept_network_query_invite.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RedeemInviteResponse(
    val ip: String,
    val invite: RedeemInviteDto,
    val pubkey: String?
)
