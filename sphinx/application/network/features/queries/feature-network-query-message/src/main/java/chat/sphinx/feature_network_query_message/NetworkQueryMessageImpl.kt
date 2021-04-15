package chat.sphinx.feature_network_query_message

import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_message.model.GetMessagesResponse
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_message.model.GetMessagesRelayResponse
import chat.sphinx.feature_network_query_message.model.GetPaymentsRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQueryMessageImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryMessage() {

    companion object {
        private const val ENDPOINT_MSGS = "/msgs"
        private const val ENDPOINT_MESSAGE = "/message"
        private const val ENDPOINT_MESSAGES = "${ENDPOINT_MESSAGE}s"
        private const val ENDPOINT_PAYMENT = "/payment"
        private const val ENDPOINT_PAYMENTS = "${ENDPOINT_PAYMENT}s"
    }

    ///////////
    /// GET ///
    ///////////
    override fun getMessages(
        messagePagination: MessagePagination?,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<GetMessagesResponse, ResponseError>> =
        networkRelayCall.get(
            jsonAdapter = GetMessagesRelayResponse::class.java,
            relayEndpoint = ENDPOINT_MSGS + (messagePagination?.value ?: ""),
            relayData = relayData
        )

    override fun getPayments(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<List<MessageDto>, ResponseError>> =
        networkRelayCall.get(
            jsonAdapter = GetPaymentsRelayResponse::class.java,
            relayEndpoint = ENDPOINT_PAYMENTS,
            relayData = relayData
        )

    ///////////
    /// PUT ///
    ///////////

    ////////////
    /// POST ///
    ////////////
//    app.post('/messages', messages.sendMessage)
//    app.post('/messages/:chat_id/read', messages.readMessages)
//    app.post('/messages/clear', messages.clearMessages)
//    app.post('/payment', payments.sendPayment)

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/message/:id', messages.deleteMessage)
}
