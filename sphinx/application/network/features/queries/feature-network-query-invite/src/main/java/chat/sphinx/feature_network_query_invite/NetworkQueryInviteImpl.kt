package chat.sphinx.feature_network_query_invite

import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_invite.model.*
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_invite.model.PayInviteResponse
import chat.sphinx.feature_network_query_invite.model.RedeemInviteRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_invite.InviteString
import kotlinx.coroutines.flow.Flow

class NetworkQueryInviteImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryInvite() {

    companion object {
        private const val ENDPOINT_INVITES = "/invites"
        private const val ENDPOINT_SIGNUP = "/api/v1/signup"
        private const val ENDPOINT_SIGNUP_FINISH = "/invites/finish"
        private const val ENDPOINT_LOWEST_PRICE = "/api/v1/nodes/pricing"
        private const val ENDPOINT_INVITE_PAY = "/invites/%s/pay"

        private const val HUB_URL = "https://hub.sphinx.chat"
    }

    override fun getLowestNodePrice(): Flow<LoadResponse<HubLowestNodePriceResponse, ResponseError>> {
        return networkRelayCall.get(
            url = HUB_URL + ENDPOINT_LOWEST_PRICE,
            responseJsonClass = HubLowestNodePriceResponse::class.java,
        )
    }


    override fun redeemInvite(
        inviteString: InviteString
    ): Flow<LoadResponse<HubRedeemInviteResponse, ResponseError>> {
        return networkRelayCall.post(
            url = HUB_URL + ENDPOINT_SIGNUP,
            responseJsonClass = HubRedeemInviteResponse::class.java,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(
                Pair("invite_string", inviteString.value),
            )
        )
    }

    // TODO: Refactor to use post instead of relayUnauthenticatedPost
    override fun finishInvite(
        inviteString: String
    ): Flow<LoadResponse<RedeemInviteResponseDto, ResponseError>> {
        return networkRelayCall.relayPost(
            responseJsonClass = RedeemInviteRelayResponse::class.java,
            relayEndpoint = ENDPOINT_SIGNUP_FINISH,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(
                Pair("invite_string", inviteString),
            )
        )
    }

    override fun payInvite(
        inviteString: InviteString
    ): Flow<LoadResponse<PayInviteDto, ResponseError>> {
        return networkRelayCall.relayPost(
            responseJsonClass = PayInviteResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_INVITE_PAY, inviteString.value),
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", ""))
        )
    }

    ///////////
    /// GET ///
    ///////////

    ///////////
    /// PUT ///
    ///////////

    ////////////
    /// POST ///
    ////////////
//    app.post('/invites', invites.createInvite)
//    app.post('/invites/:invite_string/pay', invites.payInvite)
//    app.post('/invites/finish', invites.finishInvite)

    //////////////
    /// DELETE ///
    //////////////
}
