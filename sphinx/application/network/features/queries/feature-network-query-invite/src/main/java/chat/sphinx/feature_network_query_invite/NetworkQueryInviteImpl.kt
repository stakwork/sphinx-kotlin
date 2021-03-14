package chat.sphinx.feature_network_query_invite

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_relay.RelayDataHandler
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

class NetworkQueryInviteImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler
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