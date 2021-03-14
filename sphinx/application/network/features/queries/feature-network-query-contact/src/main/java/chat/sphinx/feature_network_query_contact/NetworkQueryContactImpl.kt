package chat.sphinx.feature_network_query_contact

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_relay.RelayDataHandler
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

class NetworkQueryContactImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler
): NetworkQueryContact() {

    companion object {
        private const val ENDPOINT_CONTACTS = "/contacts"
    }

    ///////////
    /// GET ///
    ///////////
//    app.get('/contacts', contacts.getContacts)

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
