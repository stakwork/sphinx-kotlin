package chat.sphinx.concept_network_query_message

import chat.sphinx.concept_network_query_message.model.GetMessagesResponse
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import com.sun.net.httpserver.Authenticator
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryMessage {

    ///////////
    /// GET ///
    ///////////
    abstract fun getMessages(
        messagePagination: MessagePagination?,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<GetMessagesResponse, ResponseError>>

    abstract fun getPayments(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<List<MessageDto>, ResponseError>>

    ///////////
    /// PUT ///
    ///////////

    ////////////
    /// POST ///
    ////////////
//    app.post('/messages', messages.sendMessage)
//    app.post('/messages/:chat_id/read', messages.readMessages)
    abstract fun readMessages(
        chatId: ChatId,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<Any?, ResponseError>>

//    app.post('/messages/clear', messages.clearMessages)
//    app.post('/payment', payments.sendPayment)

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/message/:id', messages.deleteMessage)
}
