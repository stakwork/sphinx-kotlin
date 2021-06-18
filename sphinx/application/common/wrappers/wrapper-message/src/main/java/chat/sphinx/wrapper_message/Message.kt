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
import chat.sphinx.wrapper_message.media.getHostFromMediaToken
import chat.sphinx.wrapper_message.media.getMUIDFromMediaToken
import chat.sphinx.wrapper_message.media.getMediaAttributeWithName

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrieveTextToShow(): String? =
    messageContentDecrypted?.let { decrypted ->
        // TODO Handle podcast clips `clip::.....`
        if (giphyData != null) {
            return giphyData?.text
        }
        decrypted.value

//            ?.text
//            ?: if (podBoost == null) {
//                decrypted.value
//            } else {
//                null
//            }
//            ?.toString()
//            ?:
//            decrypted.value
    }

//Paid types
inline val Message.isPaidMessage: Boolean
    get() = type.isAttachment() && mediaPrice > 0

//Attachment types
inline val Message.isImage: Boolean
    get() = messageMedia?.mediaType?.value?.contains("image") == true

//Media attributes
inline val Message.mediaUrl: String? 
    get() = messageMedia
        ?.mediaToken
        ?.let { mediaToken ->
            mediaToken
                .getHostFromMediaToken()
                ?.let { host ->
                    "https://$host/file/${mediaToken.value}"
                }
        }

inline val Message.mediaUniqueID: String?
    get() = messageMedia
        ?.mediaToken
        ?.getMUIDFromMediaToken()

inline val Message.mediaPrice: Int
    get() = messageMedia
        ?.mediaToken
        ?.getMediaAttributeWithName("amt")
        ?.toIntOrNull()
        ?: 0

inline val Message.mediaKey: String? 
    get() = messageMedia?.mediaKey?.value

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
    abstract val replyMessage: Message?

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
                }                                                                   &&
                other.replyMessage                  == replyMessage
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
        result = 31 * result + replyMessage.hashCode()
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
                "reactions=$reactions,replyMessage=$replyMessage)"
    }
}
