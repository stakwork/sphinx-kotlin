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
    val uuid: MessageUUID?,
    val chatId: ChatId,
    val type: MessageType,
    val sender: ContactId,
    val receiver: ContactId?,
    val amount: Sat,
    val paymentHash: LightningPaymentHash?,
    val paymentRequest: LightningPaymentRequest?,
    val date: DateTime,
    val expirationDate: DateTime?,
    val messageContent: MessageContent?,
    val status: MessageStatus,
    val statusMap: Map<ContactId, MessageStatus>?,
    val mediaKey: MediaKey?,
    val mediaType: MediaType?,
    val mediaToken: MediaToken?,
    val seen: Seen,
    val senderAlias: SenderAlias?,
    val senderPic: PhotoUrl?,
//    val mediaTerms: String?, // TODO: Ask Tomas what this field is for
//    val receipt: String?, // TODO: Ask Tomas what this field is for
    val originalMUID: MessageMUID?,
    val replyUUID: ReplyUUID?,
) {
    var messageContentDecrypted: MessageContentDecrypted? = null
        private set

    fun setMessageContentDecrypted(messageContentDecrypted: MessageContentDecrypted) {
        this.messageContentDecrypted = messageContentDecrypted
    }

    var decryptionError: Boolean = false
        private set
    var decryptionException: Exception? = null
        private set

    fun setDecryptionError(e: Exception?) {
        decryptionError = true
        decryptionException = e
    }
}
