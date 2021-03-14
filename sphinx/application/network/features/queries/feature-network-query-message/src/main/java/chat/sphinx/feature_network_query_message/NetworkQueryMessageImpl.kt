package chat.sphinx.feature_network_query_message

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_relay.RelayDataHandler
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

class NetworkQueryMessageImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler
): NetworkQueryMessage() {

    companion object {
        private const val ENDPOINT_MSGS = "/msgs"
        private const val ENDPOINT_MESSAGE = "/message"
        private const val ENDPOINT_MESSAGES = "/messages"
    }

    ///////////
    /// GET ///
    ///////////
//    app.get('/msgs', messages.getMsgs)

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