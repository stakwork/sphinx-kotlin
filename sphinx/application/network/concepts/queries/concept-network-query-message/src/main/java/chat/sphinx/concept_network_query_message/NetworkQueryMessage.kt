package chat.sphinx.concept_network_query_message

import chat.sphinx.concept_network_query_contact.model.GetContactsResponse
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryMessage {

    ///////////
    /// GET ///
    ///////////
    abstract fun getContacts(): Flow<KotlinResponse<GetContactsResponse>>

    abstract fun getContacts(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl
    ): Flow<KotlinResponse<GetContactsResponse>>

    ///////////
    /// PUT ///
    ///////////

    ////////////
    /// POST ///
    ////////////
//    app.post('/messages', messages.sendMessage)
//    app.post('/messages/:chat_id/read', messages.readMessages)
//    app.post('/messages/clear', messages.clearMessages)

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/message/:id', messages.deleteMessage)
}
