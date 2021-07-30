package chat.sphinx.feature_network_query_message

import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_message.model.*
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_message.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_message.isMemberApprove
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NetworkQueryMessageImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryMessage() {

    companion object {
        private const val ENDPOINT_ATTACHMENT = "/attachment"

        private const val ENDPOINT_MSGS = "/msgs"
        private const val ENDPOINT_MESSAGE = "/message"
        private const val ENDPOINT_DELETE_MESSAGE = "/message/%d"
        private const val ENDPOINT_MEMBER_APPROVED = "/member/%d/approved/%d"
        private const val ENDPOINT_MEMBER_REJECTED = "/member/%d/rejected/%d"
        private const val ENDPOINT_MESSAGES_READ = "/messages/%d/read"
        private const val ENDPOINT_MESSAGES = "${ENDPOINT_MESSAGE}s"
        private const val ENDPOINT_PAYMENT = "/payment"
        private const val ENDPOINT_PAYMENTS = "${ENDPOINT_PAYMENT}s"
    }

    override fun getMessages(
        messagePagination: MessagePagination?,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<GetMessagesResponse, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = GetMessagesRelayResponse::class.java,
            relayEndpoint = ENDPOINT_MSGS + (messagePagination?.value ?: ""),
            relayData = relayData
        )

    override fun getPayments(
        offset: Int,
        limit: Int,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<List<TransactionDto>, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = GetPaymentsRelayResponse::class.java,
            relayEndpoint = "$ENDPOINT_PAYMENTS?offset=$offset&limit=$limit",
            relayData = relayData
        )

    override fun sendMessage(
        postMessageDto: PostMessageDto,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<MessageDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = MessageRelayResponse::class.java,
            relayEndpoint = if (postMessageDto.media_key_map != null) {
                ENDPOINT_ATTACHMENT
            } else {
                ENDPOINT_MESSAGES
            },
            requestBodyJsonClass = PostMessageDto::class.java,
            requestBody = postMessageDto,
            relayData = relayData
        )

    override fun sendPayment(
        postPaymentDto: PostPaymentDto,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<MessageDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = MessageRelayResponse::class.java,
            relayEndpoint = ENDPOINT_PAYMENT,
            requestBodyJsonClass = PostPaymentDto::class.java,
            requestBody = postPaymentDto,
            relayData = relayData
        )

    override fun sendKeySendPayment(
        postPaymentDto: PostPaymentDto,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<KeySendPaymentDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = KeySendPaymentRelayResponse::class.java,
            relayEndpoint = ENDPOINT_PAYMENT,
            requestBodyJsonClass = PostPaymentDto::class.java,
            requestBody = postPaymentDto,
            relayData = relayData
        )

    override fun boostMessage(
        chatId: ChatId,
        pricePerMessage: Sat,
        escrowAmount: Sat,
        tipAmount: Sat,
        messageUUID: MessageUUID,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<MessageDto, ResponseError>> {
        val postBoostMessageDto: PostBoostMessage = try {
            PostBoostMessage(
                chat_id = chatId.value,
                amount = pricePerMessage.value + escrowAmount.value + tipAmount.value,
                message_price = pricePerMessage.value + escrowAmount.value,
                reply_uuid = messageUUID.value
            )
        } catch (e: IllegalArgumentException) {
            return flowOf(Response.Error(ResponseError("Incorrect Arguments provided", e)))
        }

        return networkRelayCall.relayPost(
            responseJsonClass = MessageRelayResponse::class.java,
            relayEndpoint = ENDPOINT_MESSAGES,
            requestBodyJsonClass = PostBoostMessage::class.java,
            requestBody = postBoostMessageDto,
            relayData = relayData
        )
    }

    override fun readMessages(
        chatId: ChatId,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<Any?, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = ReadMessagesRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_MESSAGES_READ, chatId.value),
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
            relayData = relayData
        )

//    app.post('/messages/clear', messages.clearMessages)

    //////////////
    /// DELETE ///
    //////////////
    /**
     * Deletes a message with the id [MessageId]
     *
     * DELETE /message/$messageId
     */
    override fun deleteMessage(
        messageId: MessageId,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<MessageDto, ResponseError>> =
        networkRelayCall.relayDelete(
            responseJsonClass = MessageRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_DELETE_MESSAGE, messageId.value),
            requestBodyJsonClass = null,
            requestBody = null,
            relayData = relayData
        )


    override fun processMemberRequest(
        contactId: ContactId,
        messageId: MessageId,
        type: MessageType,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<PutMemberResponseDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = PutMemberRelayResponse::class.java,
            relayEndpoint = if (type.isMemberApprove()) {
                String.format(ENDPOINT_MEMBER_APPROVED, contactId.value, messageId.value)
            } else {
                String.format(ENDPOINT_MEMBER_REJECTED, contactId.value, messageId.value)
            },
            requestBody = null,
            relayData = relayData
        )
}
