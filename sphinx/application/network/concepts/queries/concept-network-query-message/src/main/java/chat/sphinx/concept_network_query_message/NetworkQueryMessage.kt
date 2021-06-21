package chat.sphinx.concept_network_query_message

import chat.sphinx.concept_network_query_message.model.GetMessagesResponse
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.concept_network_query_message.model.PostMessageDto
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryMessage {

    ///////////
    /// GET ///
    ///////////
    abstract fun getMessages(
        messagePagination: MessagePagination?,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<GetMessagesResponse, ResponseError>>

    abstract fun getPayments(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<List<MessageDto>, ResponseError>>

    ///////////
    /// PUT ///
    ///////////

    ////////////
    /// POST ///
    ////////////
//    app.post('/messages', messages.sendMessage)
    abstract fun sendMessage(
        postMessageDto: PostMessageDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<MessageDto, ResponseError>>

    abstract fun boostMessage(
        chatId: ChatId,
        pricePerMessage: Sat,
        escrowAmount: Sat,
        tipAmount: Sat,
        messageUUID: MessageUUID,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<MessageDto, ResponseError>>

//    app.post('/messages/:chat_id/read', messages.readMessages)
    abstract fun readMessages(
        chatId: ChatId,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<Any?, ResponseError>>

//    app.post('/messages/clear', messages.clearMessages)
//    app.post('/payment', payments.sendPayment)

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/message/:id', messages.deleteMessage)
}
