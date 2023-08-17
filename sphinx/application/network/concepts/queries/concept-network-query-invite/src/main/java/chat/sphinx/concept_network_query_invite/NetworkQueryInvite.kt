package chat.sphinx.concept_network_query_invite

import chat.sphinx.concept_network_query_invite.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_invite.InviteString
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryInvite {

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

    abstract fun getLowestNodePrice(): Flow<LoadResponse<HubLowestNodePriceResponse, ResponseError>>

    // TODO: Return RedeemInviteResponse
    abstract fun redeemInvite(
        inviteString: InviteString
    ): Flow<LoadResponse<HubRedeemInviteResponse, ResponseError>>

    abstract fun finishInvite(
        inviteString: String
    ): Flow<LoadResponse<RedeemInviteResponseDto, ResponseError>>

    abstract fun payInvite(
        inviteString: InviteString
    ): Flow<LoadResponse<PayInviteDto, ResponseError>>

    //////////////
    /// DELETE ///
    //////////////
}
