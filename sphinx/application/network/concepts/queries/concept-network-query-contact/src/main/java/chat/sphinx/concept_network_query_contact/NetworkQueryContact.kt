package chat.sphinx.concept_network_query_contact

import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryContact {

    ///////////
    /// GET ///
    ///////////
    abstract fun getContacts(): Flow<KotlinResponse<List<ContactDto>>>
    abstract fun getContacts(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl
    ): Flow<KotlinResponse<List<ContactDto>>>

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
