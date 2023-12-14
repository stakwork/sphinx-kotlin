package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.Seen
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningPaymentHash
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.CallLinkMessage
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_message_media.MessageMedia

class NewMessage(
    override val id: MessageId,
    override val uuid: MessageUUID? = null,
    override val chatId: ChatId,
    override val type: MessageType,
    override val sender: ContactId,
    override val receiver: ContactId? = null,
    override val amount: Sat,
    override val paymentHash: LightningPaymentHash? = null,
    override val paymentRequest: LightningPaymentRequest? = null,
    override val date: DateTime,
    override val expirationDate: DateTime? = null,
    override val messageContent: MessageContent? = null,
    override val status: MessageStatus,
    override val seen: Seen,
    override val senderAlias: SenderAlias? = null,
    override val senderPic: PhotoUrl? = null,
    override val originalMUID: MessageMUID? = null,
    override val replyUUID: ReplyUUID? = null,
    override val flagged: Flagged,
    override val recipientAlias: RecipientAlias? = null,
    override val recipientPic: PhotoUrl? = null,
    override val person: MessagePerson? = null,
    override val threadUUID: ThreadUUID? = null,
    override val errorMessage: ErrorMessage? = null,
    override val isPinned: Boolean = false,

    override val messageContentDecrypted: MessageContentDecrypted? = null,
    override val messageDecryptionError: Boolean = false,
    override val messageDecryptionException: Exception? = null,
    override val messageMedia: MessageMedia? = null,
    override val feedBoost: FeedBoost? = null,
    override val callLinkMessage: CallLinkMessage? = null,
    override val podcastClip: PodcastClip? = null,
    override val giphyData: GiphyData? = null,
    override val reactions: List<Message>? = null,
    override val purchaseItems: List<Message>? = null,
    override val replyMessage: Message? = null,
    override val thread: List<Message>? = null
) : Message() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewMessage

        if (id != other.id) return false
        // Add checks for other properties as necessary

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        // Add hash code calculation for other properties as necessary
        return result
    }

    override fun toString(): String {
        return "NewMessage(id=$id, uuid=$uuid, chatId=$chatId, type=$type, sender=$sender, " +
                "receiver=$receiver, amount=$amount, paymentHash=$paymentHash, " +
                "paymentRequest=$paymentRequest, date=$date, expirationDate=$expirationDate, " +
                "messageContent=$messageContent, status=$status, seen=$seen, " +
                "senderAlias=$senderAlias, senderPic=$senderPic, originalMUID=$originalMUID, " +
                "replyUUID=$replyUUID, flagged=$flagged, recipientAlias=$recipientAlias, " +
                "recipientPic=$recipientPic, person=$person, threadUUID=$threadUUID, " +
                "errorMessage=$errorMessage, isPinned=$isPinned, " +
                "messageContentDecrypted=$messageContentDecrypted, " +
                "messageDecryptionError=$messageDecryptionError, " +
                "messageDecryptionException=$messageDecryptionException, " +
                "messageMedia=$messageMedia, feedBoost=$feedBoost, callLinkMessage=$callLinkMessage, " +
                "podcastClip=$podcastClip, giphyData=$giphyData, reactions=$reactions, " +
                "purchaseItems=$purchaseItems, replyMessage=$replyMessage, thread=$thread)"
    }
}
