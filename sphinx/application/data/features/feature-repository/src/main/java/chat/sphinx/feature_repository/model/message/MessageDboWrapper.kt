package chat.sphinx.feature_repository.model.message

import chat.sphinx.conceptcoredb.MessageDbo
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
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.MessageMedia

class MessageDboWrapper(
    val messageDbo: MessageDbo
): Message() {
    override val id: MessageId
        get() = messageDbo.id
    override val uuid: MessageUUID?
        get() = messageDbo.uuid
    override val chatId: ChatId
        get() = messageDbo.chat_id
    override val type: MessageType
        get() = messageDbo.type
    override val sender: ContactId
        get() = messageDbo.sender
    override val receiver: ContactId?
        get() = messageDbo.receiver_
    override val amount: Sat
        get() = messageDbo.amount
    override val paymentHash: LightningPaymentHash?
        get() = messageDbo.payment_hash
    override val paymentRequest: LightningPaymentRequest?
        get() = messageDbo.payment_request
    override val date: DateTime
        get() = messageDbo.date
    override val expirationDate: DateTime?
        get() = messageDbo.expiration_date
    override val messageContent: MessageContent?
        get() = messageDbo.message_content
    override val status: MessageStatus
        get() = messageDbo.status
    override val seen: Seen
        get() = messageDbo.seen
    override val senderAlias: SenderAlias?
        get() = messageDbo.sender_alias
    override val senderPic: PhotoUrl?
        get() = messageDbo.sender_pic
    override val originalMUID: MessageMUID?
        get() = messageDbo.original_muid
    override val replyUUID: ReplyUUID?
        get() = messageDbo.reply_uuid
    override val flagged: Flagged
        get() = messageDbo.flagged
    override val recipientAlias: RecipientAlias?
        get() = messageDbo.recipient_alias
    override val recipientPic: PhotoUrl?
        get() = messageDbo.recipient_pic
    override val person: MessagePerson?
        get() = messageDbo.person
    override val threadUUID: ThreadUUID?
        get() = messageDbo.thread_uuid


    @Volatile
    @Suppress("PropertyName")
    var _messageContentDecrypted: MessageContentDecrypted? = messageDbo.message_content_decrypted
    override val messageContentDecrypted: MessageContentDecrypted?
        get() = _messageContentDecrypted

    @Volatile
    @Suppress("PropertyName")
    var _messageDecryptionError: Boolean = false
    override val messageDecryptionError: Boolean
        get() = _messageDecryptionError

    @Volatile
    @Suppress("PropertyName")
    var _messageDecryptionException: Exception? = null
    override val messageDecryptionException: Exception?
        get() = _messageDecryptionException

    @Volatile
    @Suppress("PropertyName")
    var _messageMedia: MessageMediaDboWrapper? = null
    override val messageMedia: MessageMedia?
        get() = _messageMedia

    @Volatile
    @Suppress("PropertyName")
    var _feedBoost: FeedBoost? = null
    override val feedBoost: FeedBoost?
        get() = _feedBoost

    @Volatile
    @Suppress("PropertyName")
    var _callLinkMessage: CallLinkMessage? = null
    override val callLinkMessage: CallLinkMessage?
        get() = _callLinkMessage

    @Volatile
    @Suppress("PropertyName")
    var _podcastClip: PodcastClip? = null
    override val podcastClip: PodcastClip?
        get() = _podcastClip

    @Volatile
    @Suppress("PropertyName")
    var _giphyData: GiphyData? = null
    override val giphyData: GiphyData?
        get() = _giphyData

    @Volatile
    @Suppress("PropertyName")
    var _reactions: List<Message>? = null
    override val reactions: List<Message>?
        get() = _reactions

    @Volatile
    @Suppress("PropertyName")
    var _purchaseItems: List<Message>? = null
    override val purchaseItems: List<Message>?
        get() = _purchaseItems

    @Volatile
    @Suppress("PropertyName")
    var _replyMessage: Message? = null
    override val replyMessage: Message?
        get() = _replyMessage

    @Volatile
    @Suppress("PropertyName")
    var _thread: List<Message>? = null
    override val thread: List<Message>?
        get() = _thread

    @Volatile
    @Suppress("PropertyName")
    var _isPinned: Boolean = false
    override val isPinned: Boolean
        get() = _isPinned
}
