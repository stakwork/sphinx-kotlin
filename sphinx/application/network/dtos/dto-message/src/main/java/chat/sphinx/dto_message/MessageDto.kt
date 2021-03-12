package chat.sphinx.dto_message

import chat.sphinx.dto_chat.ChatDto
import chat.sphinx.dto_contact.ContactDto
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.lightning.LightningPaymentHash
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.MilliSat
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.parent.ParentId
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_message.*

class MessageDto(
    val id: MessageId,
    val uuid: MessageUUID?,
    val chatId: ChatId,
    val type: MessageType,
    val sender: ContactId,
//    val receiver: Int?,
    val amount: Sat,
    val amountMSat: MilliSat,
    val paymentHash: LightningPaymentHash?,
    val paymentRequest: LightningPaymentRequest?,
    val date: DateTime,
    val expirationDate: DateTime?,
    val messageContent: MessageContent?,
    val remoteMessageContent: RemoteMessageContent?,
    val status: MessageStatus,
    val statusMap: MessageStatusMap?,
    val parentId: ParentId?,
    val subscriptionId: SubscriptionId?,
//    val mediaKey: String?,
//    val mediaType: Int?,
//    val mediaToken: String?,
    val seen: Boolean,
    val createdAt: DateTime,
    val updatedAt: DateTime,
    val senderAlias: ContactAlias?,
    val senderPic: PhotoUrl?,
    val originalMUID: MessageUUID?,
    val replyUUID: MessageUUID?,
    val networkType: NetworkType?,
    val chat: ChatDto?,
    val contact: ContactDto?,
)
