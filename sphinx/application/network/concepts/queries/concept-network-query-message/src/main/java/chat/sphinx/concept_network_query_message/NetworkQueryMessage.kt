package chat.sphinx.concept_network_query_message

import chat.sphinx.concept_network_query_message.model.GetMessagesResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryMessage {

    ///////////
    /// GET ///
    ///////////
    abstract fun getMessages(
        messagePagination: MessagePagination?
    ): Flow<LoadResponse<GetMessagesResponse, ResponseError>>

    abstract fun getMessages(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
        messagePagination: MessagePagination?,
    ): Flow<LoadResponse<GetMessagesResponse, ResponseError>>

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
