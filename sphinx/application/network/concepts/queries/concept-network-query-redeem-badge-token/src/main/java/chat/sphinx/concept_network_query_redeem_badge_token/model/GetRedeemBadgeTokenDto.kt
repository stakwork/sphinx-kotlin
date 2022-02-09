package chat.sphinx.concept_network_query_redeem_badge_token.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetRedeemBadgeTokenDto(
    val key: String,
    val body: String,
    val path: String,
    val method: String,
)