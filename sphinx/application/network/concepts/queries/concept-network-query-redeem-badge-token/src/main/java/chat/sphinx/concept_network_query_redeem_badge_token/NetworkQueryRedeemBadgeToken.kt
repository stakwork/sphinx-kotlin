package chat.sphinx.concept_network_query_redeem_badge_token

import chat.sphinx.concept_network_query_redeem_badge_token.model.RedeemBadgeTokenDto
import chat.sphinx.concept_network_query_redeem_badge_token.model.GetRedeemBadgeTokenDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryRedeemBadgeToken{
    abstract fun getRedeemBadgeTokenByKey(
        host: String,
        key: String
    ): Flow<LoadResponse<GetRedeemBadgeTokenDto, ResponseError>>

    abstract fun redeemBadgeToken(
        data: RedeemBadgeTokenDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<Any, ResponseError>>

} 

