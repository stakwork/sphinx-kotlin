package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.Seen
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.lightning.LightningPaymentHash
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId

data class Message(
    val id: MessageId,
    val uuid: MessageUUID,
    val replyUUID: MessageUUID?,
    val type: MessageType,
    val sender: ContactId,
    val senderAlias: SenderAlias?,
    val senderPic: PhotoUrl?,
    val receiver: ContactId?,
    val amount: Sat,
    val paymentHash: LightningPaymentHash?,
    val paymentRequest: LightningPaymentRequest?,
    val messageContent: MessageContent?,
    val remoteMessageContent: RemoteMessageContent?,
    val status: MessageStatus,
    val date: DateTime,
    val expirationDate: DateTime?,
//    val mediaTerms: String?, // TODO: Ask Tomas what this field is for
//    val receipt: String?, // TODO: Ask Tomas what this field is for
    val mediaToken: String?,
    val mediaKey: String?,
    val mediaType: String?,
    val originalMUID: String?,
    val seen: Seen,
    val chatId: ChatId?
)
