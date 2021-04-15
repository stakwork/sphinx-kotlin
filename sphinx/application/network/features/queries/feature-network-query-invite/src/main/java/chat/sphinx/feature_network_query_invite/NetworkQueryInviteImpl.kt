package chat.sphinx.feature_network_query_invite

import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_relay_call.NetworkRelayCall

class NetworkQueryInviteImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryInvite() {

    companion object {
        private const val ENDPOINT_INVITES = "/invites"
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
