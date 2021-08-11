package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.Seen
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningPaymentHash
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.*
import chat.sphinx.wrapper_message_media.MessageMedia
import chat.sphinx.wrapper_message_media.isImage

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrieveTextToShow(): String? =
    messageContentDecrypted?.let { decrypted ->
        // TODO Handle podcast clips `clip::.....`
        if (giphyData != null) {
            return giphyData?.text
        }
        if (podBoost != null) {
            return null
        }
        if (isSphinxCallLink) {
            return null
        }
        decrypted.value
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrieveImageUrlAndMessageMedia(): Pair<String, MessageMedia?>? {
    var mediaData: Pair<String, MessageMedia?>? = null

    if (this.type.isDirectPayment()) {
        return null
    }

    giphyData?.let { giphyData ->
        mediaData = giphyData.retrieveImageUrlAndMessageMedia()
    } ?: messageMedia?.let { media ->
        if (media.mediaType.isImage && !isPaidMessage) {

            // always prefer loading a file if it exists over loading a url
            if (media.localFile != null) {
                mediaData = Pair(
                    media.url?.value?.let { if (it.isEmpty()) null else it } ?: "http://127.0.0.1",
                    media,
                )
            } else {
                media.url?.let { mediaUrl ->
                    mediaData = Pair(mediaUrl.value, media)
                }
            }

        }
    }
    return mediaData
}

@Suppress("NOTHING_TO_INLINE")
inline fun Message.retrieveSphinxCallLink(): SphinxCallLink? =
    messageContentDecrypted?.let { decrypted ->
        decrypted.value.toSphinxCallLink()?.let { sphinxCallLink ->
            return sphinxCallLink
        }
        null
    }

//Message Actions
inline val Message.isBoostAllowed: Boolean
    get() = status.isReceived() &&
            !type.isInvoice() &&
            !type.isDirectPayment() &&
            (uuid?.value ?: "").isNotEmpty()

inline val Message.isCopyAllowed: Boolean
    get() = (this.retrieveTextToShow() ?: "").isNotEmpty()

inline val Message.isReplyAllowed: Boolean
    get() = (type.isAttachment() || type.isMessage()) &&
            (uuid?.value ?: "").isNotEmpty()

inline val Message.isResendAllowed: Boolean
    get() = type.isMessage() && status.isFailed()

//Paid types
inline val Message.isPaidMessage: Boolean
    get() = type.isAttachment() && (messageMedia?.price?.value ?: 0L) > 0L

inline val Message.isSphinxCallLink: Boolean
    get() = type.isMessage() && (messageContentDecrypted?.value?.isValidSphinxCallLink == true)

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

    companion object {
        @Suppress("ObjectPropertyName")
        private const val _17 = 17
        @Suppress("ObjectPropertyName")
        private const val _31 = 31
    }

    override fun hashCode(): Int {
        var result = _17
        result = _31 * result + id.hashCode()
        result = _31 * result + uuid.hashCode()
        result = _31 * result + chatId.hashCode()
        result = _31 * result + type.hashCode()
        result = _31 * result + sender.hashCode()
        result = _31 * result + receiver.hashCode()
        result = _31 * result + amount.hashCode()
        result = _31 * result + paymentHash.hashCode()
        result = _31 * result + paymentRequest.hashCode()
        result = _31 * result + date.hashCode()
        result = _31 * result + expirationDate.hashCode()
        result = _31 * result + messageContent.hashCode()
        result = _31 * result + status.hashCode()
        result = _31 * result + seen.hashCode()
        result = _31 * result + senderAlias.hashCode()
        result = _31 * result + senderPic.hashCode()
        result = _31 * result + originalMUID.hashCode()
        result = _31 * result + replyUUID.hashCode()
        result = _31 * result + messageContentDecrypted.hashCode()
        result = _31 * result + messageDecryptionError.hashCode()
        result = _31 * result + messageDecryptionException.hashCode()
        result = _31 * result + messageMedia.hashCode()
        result = _31 * result + podBoost.hashCode()
        result = _31 * result + giphyData.hashCode()
        reactions?.forEach { result = _31 * result + it.hashCode() }
        result = _31 * result + replyMessage.hashCode()
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
