package chat.sphinx.concept_network_query_invite.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RedeemInviteDto(
    val id: Long,
    val node_id: Long,
    val invite_status: Int,
    val pubkey: String?,
    val expires_on: String?,
    val fee_paid: Long?,
    val message: String?,
    var nickname: String?,
    var pin: String?,
    val created_at: String,
    val updated_at: String,
    var contact_nickname: String?,
    var invoice: String?,
    var action: String?,
    var route_hint: String?,
)
