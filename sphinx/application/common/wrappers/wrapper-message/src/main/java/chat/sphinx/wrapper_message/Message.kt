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
import chat.sphinx.wrapper_message.media.MessageMedia

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
    val seen: Seen,
    val senderAlias: SenderAlias?,
    val senderPic: PhotoUrl?,
//    val mediaTerms: String?, // TODO: Ask Tomas what this field is for
//    val receipt: String?, // TODO: Ask Tomas what this field is for
    val originalMUID: MessageMUID?,
    val replyUUID: ReplyUUID?,
) {
    @Volatile
    var messageContentDecrypted: MessageContentDecrypted? = null
        private set

    fun setMessageContentDecrypted(messageContentDecrypted: MessageContentDecrypted): Message {
        this.messageContentDecrypted = messageContentDecrypted
        return this
    }

    @Volatile
    var messageDecryptionError: Boolean = false
        private set

    @Volatile
    var messageDecryptionException: Exception? = null
        private set

    fun setDecryptionError(e: Exception?): Message {
        messageDecryptionError = true
        messageDecryptionException = e
        return this
    }

    @Volatile
    var messageMedia: MessageMedia? = null

    fun setMessageMedia(messageMedia: MessageMedia): Message {
        this.messageMedia = messageMedia
        return this
    }

    @Volatile
    var podBoost: PodBoost? = null
        private set

    fun setPodBoost(podBoost: PodBoost): Message {
        this.podBoost = podBoost
        return this
    }

    @Volatile
    var giphyData: GiphyData? = null
        private set

    fun setGiphyData(giphyData: GiphyData): Message {
        this.giphyData = giphyData
        return this
    }
}
