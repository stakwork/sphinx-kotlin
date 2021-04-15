package chat.sphinx.feature_network_query_contact

import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.GetContactsResponse
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_contact.model.GetContactsRelayResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQueryContactImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryContact() {

    companion object {
        private const val ENDPOINT_CONTACTS = "/contacts"
    }

    override fun getContacts(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<GetContactsResponse, ResponseError>> =
        networkRelayCall.get(
            jsonAdapter = GetContactsRelayResponse::class.java,
            relayEndpoint = ENDPOINT_CONTACTS,
            relayData = relayData
        )

    ///////////
    /// PUT ///
    ///////////
//    app.put('/contacts/:id', contacts.updateContact)

    ////////////
    /// POST ///
    ////////////
//    app.post('/contacts/tokens', contacts.generateToken)
//    app.post('/contacts/:id/keys', contacts.exchangeKeys)
//    app.post('/contacts', contacts.createContact)

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/contacts/:id', contacts.deleteContact)
}
