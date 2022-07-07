package chat.sphinx.feature_network_query_redeem_badge_token

import chat.sphinx.concept_network_query_redeem_badge_token.NetworkQueryRedeemBadgeToken
import chat.sphinx.concept_network_query_redeem_badge_token.model.RedeemBadgeTokenDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_redeem_badge_token.model.RedeemBadgeTokenResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

class NetworkQueryRedeemBadgeTokenImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryRedeemBadgeToken() {

    companion object {
        private const val ENDPOINT_CLAIM_ON_LIQUID = "/claim_on_liquid"
    }

    override fun redeemBadgeToken(
        data: RedeemBadgeTokenDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.relayPost(
            relayEndpoint = ENDPOINT_CLAIM_ON_LIQUID,
            requestBody = data,
            requestBodyJsonClass = RedeemBadgeTokenDto::class.java,
            responseJsonClass = RedeemBadgeTokenResponse::class.java,
            relayData = relayData
        )

}
