package chat.sphinx.concept_repository_message

import chat.sphinx.concept_repository_message.model.SendPaymentRequest
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_repository_message.model.SendPayment
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.lightning.Bolt11
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningPaymentHash
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_common.payment.PaymentTemplate
import chat.sphinx.wrapper_message.FeedBoost
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.MessageContentDecrypted
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_message.Msg
import chat.sphinx.wrapper_message.MsgSender
import chat.sphinx.wrapper_message.SenderAlias
import chat.sphinx.wrapper_message.ThreadUUID
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getAllMessagesToShowByChatId(
        chatId: ChatId,
        limit: Long,
        chatThreadUUID: ThreadUUID? = null
    ): Flow<List<Message>>

    fun searchMessagesBy(chatId: ChatId, term: String): Flow<List<Message>>

    fun getMessageById(messageId: MessageId): Flow<Message?>
    fun getMessagesByIds(messagesIds: List<MessageId>): Flow<List<Message?>>
    fun getTribeLastMemberRequestBySenderAlias(alias: SenderAlias, chatId: ChatId): Flow<Message?>
    fun getMessageByUUID(messageUUID: MessageUUID): Flow<Message?>
    fun getPaymentsTotalFor(feedId: FeedId): Flow<Sat?>

    fun getThreadUUIDMessagesByChatId(chatId: ChatId): Flow<List<Message>>
    fun getThreadUUIDMessagesByUUID(chatId: ChatId, threadUUID: ThreadUUID): Flow<List<Message>>

    suspend fun getAllMessagesByUUID(messageUUIDs: List<MessageUUID>): List<Message>

    suspend fun fetchPinnedMessageByUUID(messageUUID: MessageUUID, chatId: ChatId)

    fun updateMessageContentDecrypted(messageId: MessageId, messageContentDecrypted: MessageContentDecrypted)

    suspend fun readMessages(chatId: ChatId)

    fun sendMessage(sendMessage: SendMessage?)

    suspend fun payAttachment(message: Message)

    fun sendMediaKeyOnPaidPurchase(
        msg: Msg,
        contactInfo: MsgSender,
        paidAmount: Sat
    )

    fun updatePaidMessageMediaKey(
        msg: Msg,
        contactInfo: MsgSender,
    )

    fun resendMessage(
        message: Message,
        chat: Chat,
    )

    fun flagMessage(
        message: Message,
        chat: Chat,
    )

    fun sendBoost(
        chatId: ChatId,
        boost: FeedBoost
    )

    suspend fun deleteMessage(message: Message)

    suspend fun getPaymentTemplates() : Response<List<PaymentTemplate>, ResponseError>

    suspend fun sendPayment(
        sendPayment: SendPayment?
    ): Response<Any, ResponseError>

    suspend fun sendPaymentRequest(
        requestPayment: SendPaymentRequest
    ): Response<Any, ResponseError>

    suspend fun payPaymentRequest(message: Message) : Response<Any, ResponseError>

    suspend fun payNewPaymentRequest(message: Message)

    suspend fun sendNewPaymentRequest(
        requestPayment: SendPaymentRequest
    )

    suspend fun boostMessage(
        chatId: ChatId,
        pricePerMessage: Sat,
        escrowAmount: Sat,
        messageUUID: MessageUUID,
    ): Response<Any, ResponseError>

    suspend fun sendTribePayment(
        chatId: ChatId,
        amount: Sat,
        messageUUID: MessageUUID,
        text: String,
    )

    suspend fun processMemberRequest(
        chatId: ChatId,
        messageUuid: MessageUUID?,
        memberPubKey: LightningNodePubKey?,
        type: MessageType.GroupAction,
        alias: SenderAlias?,
    )

    suspend fun upsertMqttMessage(
        msg: Msg,
        msgSender: MsgSender,
        msgType: MessageType,
        msgUuid: MessageUUID,
        msgIndex: MessageId,
        originalUuid: MessageUUID?,
        date: DateTime?,
        isSent: Boolean,
        amount: Sat?,
        paymentRequest: LightningPaymentRequest?,
        paymentHash: LightningPaymentHash?,
        bolt11: Bolt11?
    )

    suspend fun deleteMqttMessage(messageUuid: MessageUUID)
}
