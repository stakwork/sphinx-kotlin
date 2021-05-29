package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.Seen
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningPaymentHash
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_message.media.MessageMedia

/**
 * Messages are consider "paid" if they have a type equalling `ATTACHMENT`,
 * and if the price that can be extracted from the mediaToken is greater than 0.
 */
inline val Message.isPaidMessage: Boolean
    get() {
        // TODO: Implement logic at the repository level for extracting a price from the media token.
//        return type.isAttachment() && (messageMedia?.priceFromToken ?: 0) > 0
        return false
    }


//data class Message(
//    val id: MessageId,
//    val uuid: MessageUUID?,
//    val chatId: ChatId,
//    val type: MessageType,
//    val sender: ContactId,
//    val receiver: ContactId?,
//    val amount: Sat,
//    val paymentHash: LightningPaymentHash?,
//    val paymentRequest: LightningPaymentRequest?,
//    val date: DateTime,
//    val expirationDate: DateTime?,
//    val messageContent: MessageContent?,
//    val status: MessageStatus,
//    val seen: Seen,
//    val senderAlias: SenderAlias?,
//    val senderPic: PhotoUrl?,
////    val mediaTerms: String?, // TODO: Ask Tomas what this field is for
////    val receipt: String?, // TODO: Ask Tomas what this field is for
//    val originalMUID: MessageMUID?,
//    val replyUUID: ReplyUUID?,
//    val reactionIds: List<MessageId>?,
//) {
//    @Volatile
//    var messageContentDecrypted: MessageContentDecrypted? = null
//        private set
//
//    fun setMessageContentDecrypted(messageContentDecrypted: MessageContentDecrypted): Message {
//        this.messageContentDecrypted = messageContentDecrypted
//        return this
//    }
//
//    @Volatile
//    var messageDecryptionError: Boolean = false
//        private set
//
//    @Volatile
//    var messageDecryptionException: Exception? = null
//        private set
//
//    fun setDecryptionError(e: Exception?): Message {
//        messageDecryptionError = true
//        messageDecryptionException = e
//        return this
//    }
//
//    @Volatile
//    var messageMedia: MessageMedia? = null
//
//    fun setMessageMedia(messageMedia: MessageMedia): Message {
//        this.messageMedia = messageMedia
//        return this
//    }
//
//    @Volatile
//    var podBoost: PodBoost? = null
//        private set
//
//    fun setPodBoost(podBoost: PodBoost): Message {
//        this.podBoost = podBoost
//        return this
//    }
//
//    @Volatile
//    var giphyData: GiphyData? = null
//        private set
//
//    fun setGiphyData(giphyData: GiphyData): Message {
//        this.giphyData = giphyData
//        return this
//    }
//
//    @Volatile
//    var reactions: List<Message>? = null
//        private set
//
//    fun setReactions(reactions: List<Message>?) {
//        if (reactions != null && reactions.isNotEmpty()) {
//            this.reactions = reactions
//        }
//    }
//}

abstract class Message {
    abstract val id: MessageId
    abstract val uuid: MessageUUID?
    abstract val chatId: ChatId
    abstract val type: MessageType
    abstract val sender: ContactId
    abstract val receiver: ContactId?
    abstract val amount: Sat
    abstract val paymentHash: LightningPaymentHash?
    abstract val paymentRequest: LightningPaymentRequest?
    abstract val date: DateTime
    abstract val expirationDate: DateTime?
    abstract val messageContent: MessageContent?
    abstract val status: MessageStatus
    abstract val seen: Seen
    abstract val senderAlias: SenderAlias?
    abstract val senderPic: PhotoUrl?
    //    abstract val mediaTerms: String?, // TODO: Ask Tomas what this field is for
//    abstract val receipt: String?, // TODO: Ask Tomas what this field is for
    abstract val originalMUID: MessageMUID?
    abstract val replyUUID: ReplyUUID?

    abstract val messageContentDecrypted: MessageContentDecrypted?
    abstract val messageDecryptionError: Boolean
    abstract val messageDecryptionException: Exception?
    abstract val messageMedia: MessageMedia?
    abstract val podBoost: PodBoost?
    abstract val giphyData: GiphyData?
    abstract val reactions: List<Message>?

    override fun equals(other: Any?): Boolean {
        return  other                               is Message                      &&
                other.id                            == id                           &&
                other.uuid                          == uuid                         &&
                other.chatId                        == chatId                       &&
                other.type                          == type                         &&
                other.sender                        == sender                       &&
                other.receiver                      == receiver                     &&
                other.amount                        == amount                       &&
                other.paymentHash                   == paymentHash                  &&
                other.paymentRequest                == paymentRequest               &&
                other.date                          == date                         &&
                other.expirationDate                == expirationDate               &&
                other.messageContent                == messageContent               &&
                other.status                        == status                       &&
                other.seen                          == seen                         &&
                other.senderAlias                   == senderAlias                  &&
                other.senderPic                     == senderPic                    &&
                other.originalMUID                  == originalMUID                 &&
                other.replyUUID                     == replyUUID                    &&
                other.messageContentDecrypted       == messageContentDecrypted      &&
                other.messageDecryptionError        == messageDecryptionError       &&
                other.messageDecryptionException    == messageDecryptionException   &&
                other.messageMedia                  == messageMedia                 &&
                other.podBoost                      == podBoost                     &&
                other.giphyData                     == giphyData                    &&
                other.reactions.let { a ->
                    reactions.let { b ->
                        (a.isNullOrEmpty() && b.isNullOrEmpty()) ||
                        (a?.containsAll(b ?: emptyList()) == true && b?.containsAll(a) == true)
                    }
                }
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + id.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + chatId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + sender.hashCode()
        result = 31 * result + receiver.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + paymentHash.hashCode()
        result = 31 * result + paymentRequest.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + expirationDate.hashCode()
        result = 31 * result + messageContent.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + seen.hashCode()
        result = 31 * result + senderAlias.hashCode()
        result = 31 * result + senderPic.hashCode()
        result = 31 * result + originalMUID.hashCode()
        result = 31 * result + replyUUID.hashCode()
        result = 31 * result + messageContentDecrypted.hashCode()
        result = 31 * result + messageDecryptionError.hashCode()
        result = 31 * result + messageDecryptionException.hashCode()
        result = 31 * result + messageMedia.hashCode()
        result = 31 * result + podBoost.hashCode()
        result = 31 * result + giphyData.hashCode()
        reactions?.forEach { result = 31 * result + it.hashCode() }
        return result
    }

    override fun toString(): String {
        return "Message(id=$id,uuid=$uuid,chatId=$chatId,type=$type,sender=$sender,"        +
                "receiver=$receiver,amount=$amount,paymentHash=$paymentHash,"               +
                "paymentRequest=$paymentRequest,date=$date,expirationDate=$expirationDate," +
                "messageContent=$messageContent,status=$status,seen=$seen,"                 +
                "senderAlias=$senderAlias,senderPic=$senderPic,originalMUID=$originalMUID," +
                "replyUUID=$replyUUID,messageContentDecrypted=$messageContentDecrypted,"    +
                "messageDecryptionError=$messageDecryptionError,"                           +
                "messageDecryptionException=$messageDecryptionException,"                   +
                "messageMedia=$messageMedia,podBoost=$podBoost,giphyData=$giphyData,"       +
                "reactions=$reactions)"
    }
}
