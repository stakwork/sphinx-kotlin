package chat.sphinx.concept_network_query_redeem_badge_token.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RedeemBadgeTokenDto(
    val host: String,
    val amount: Int,
    val to: String,
    val asset: Int,
    val memo: String
) 
