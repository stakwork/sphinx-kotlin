package chat.sphinx.chat_common.ui.viewstate.messageholder

import androidx.annotation.IntRange
import chat.sphinx.concept_link_preview.model.*
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_common.FileSize
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.LightningNodeDescriptor
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_message.PodcastClip as PodcastClipObject
import chat.sphinx.wrapper_message.PurchaseStatus
import chat.sphinx.wrapper_message.RecipientAlias
import chat.sphinx.wrapper_message_media.FileName
import chat.sphinx.wrapper_message_media.MessageMedia
import java.io.File

sealed class LayoutState private constructor() {

    data class SearchHighlightedStatus(
        val highlightedText: String
    ): LayoutState()

    data class MessageStatusHeader(
        val senderName: String?,
        val colorKey: String,
        val showSent: Boolean,
        val showSendingIcon: Boolean,
        val showBoltIcon: Boolean,
        val showFailedContainer: Boolean,
        val showLockIcon: Boolean,
        val timestamp: String,
        val errorMessage: String?
    ): LayoutState() {
        val showReceived: Boolean
            get() = !showSent
    }

    data class InvoiceExpirationHeader(
        val showExpirationReceivedHeader: Boolean,
        val showExpirationSentHeader: Boolean,
        val showExpiredLabel: Boolean,
        val showExpiresAtLabel: Boolean,
        val expirationTimestamp: String?,
    ): LayoutState()

    data class Separator(
        val messageHolderType: MessageHolderType,
        val date: DateTime?,
    ): LayoutState()

    data class GroupActionIndicator(
        val actionType: MessageType.GroupAction,
        val chatType: ChatType?,
        val isAdminView: Boolean,
        val subjectName: String?,
    ): LayoutState()

    data class DeletedOrFlaggedMessage(
        val gravityStart: Boolean,
        val deleted: Boolean,
        val flagged: Boolean,
        val timestamp: String,
    ): LayoutState()

    data class InvoicePayment(
        val showSent: Boolean,
        val paymentDateString: String,
    ): LayoutState() {
        val showReceived: Boolean
            get() = !showSent
    }

    sealed class Bubble private constructor(): LayoutState() {

        sealed class ContainerFirst private constructor(): Bubble() {

            data class ReplyMessage(
                val showSent: Boolean,
                val sender: String,
                val colorKey: String,
                val text: String,
                val isAudio: Boolean,
                val url: String?,
                val media: MessageMedia?,
            ): ContainerFirst() {
                val showReceived: Boolean
                    get() = !showSent
            }

        }

        sealed class ContainerSecond private constructor(): Bubble() {

            data class PaidMessageSentStatus(
                val amount: Sat,
                val purchaseStatus: PurchaseStatus?,
            ): ContainerSecond() {
                val amountText: String
                    get() = amount.asFormattedString(appendUnit = true)
            }

            data class DirectPayment(
                val showSent: Boolean,
                val amount: Sat,
                val isTribe: Boolean,
                val recipientAlias: RecipientAlias?,
                val recipientPic: PhotoUrl?,
                val recipientColorKey: String,
            ): ContainerSecond() {
                val showReceived: Boolean
                    get() = !showSent

                val unitLabel: String
                    get() = amount.unit
            }

            data class Invoice(
                val showSent: Boolean,
                val amount: Sat,
                val text: String,
                val showPaidInvoiceBottomLine: Boolean,
                val hideBubbleArrows: Boolean,
                val showPayButton: Boolean,
                val showDashedBorder: Boolean,
                val showExpiredLayout: Boolean,
            ): ContainerSecond() {
                val showReceived: Boolean
                    get() = !showSent

                val unitLabel: String
                    get() = amount.unit
            }

            sealed class AudioAttachment: ContainerSecond() {

                data class FileAvailable(
                    val messageId: MessageId,
                    val file: File
                ): AudioAttachment()

                data class FileUnavailable(
                    val messageId: MessageId,
                    val showPaidOverlay: Boolean
                ): AudioAttachment()
            }

            data class ImageAttachment(
                val url: String,
                val media: MessageMedia?,
                val showPaidOverlay: Boolean,
                val isThread: Boolean
            ): ContainerSecond()

            sealed class VideoAttachment : ContainerSecond()  {
                data class FileAvailable(val file: File): VideoAttachment()
                data class FileUnavailable(val showPaidOverlay: Boolean): VideoAttachment()
            }

            sealed class FileAttachment: ContainerSecond(){

                data class FileAvailable(
                    val fileName: FileName?,
                    val fileSize: FileSize,
                    val isPdf: Boolean,
                    val pageCount: Int
                ): FileAttachment()

                data class FileUnavailable(
                    val pendingPayment: Boolean
                ) : FileAttachment()
            }

            data class PodcastBoost(
                val amount: Sat,
            ): ContainerSecond()

            data class CallInvite(
                val videoButtonVisible: Boolean
            ): ContainerSecond()

            data class BotResponse(
                val html: String
            ): ContainerSecond()

            data class PodcastClip(
                val messageId: MessageId,
                val messageUUID: MessageUUID?,
                val podcastClip: PodcastClipObject,
            ): ContainerSecond()

            // FileAttachment
            // Invoice
        }

        sealed class ContainerThird private constructor(): Bubble() {

            data class UnsupportedMessageType(
                val messageType: MessageType,
                val gravityStart: Boolean,
            ): ContainerThird()

            data class Message(
                val text: String?,
                val highlightedTexts: List<Pair<String, IntRange>>,
                val decryptionError: Boolean,
                val isThread: Boolean
            ): ContainerThird()

            data class Thread(
                val replyCount: Int,
                val users: List<ReplyUserHolder>,
                val lastReplyMessage: String?,
                val lastReplyDate: String,
                val lastReplyUser: ReplyUserHolder,
                val isSentMessage: Boolean,
                val mediaAttachment: ContainerSecond?
            )

            data class PaidMessage(
                val showSent: Boolean,
                val purchaseStatus: PurchaseStatus?
            ): ContainerThird()

            sealed class LinkPreview private constructor(): ContainerThird() {

                data class ContactPreview(
                    val alias: ContactAlias?,
                    val photoUrl: PhotoUrl?,
                    val showBanner: Boolean,

                    // Used only to anchor data for click listeners
                    val lightningNodeDescriptor: LightningNodeDescriptor
                ): LinkPreview()

                data class HttpUrlPreview(
                    val title: HtmlPreviewTitle?,
                    val domainHost: HtmlPreviewDomainHost,
                    val description: PreviewDescription?,
                    val imageUrl: PreviewImageUrl?,
                    val favIconUrl: HtmlPreviewFavIconUrl?,

                    // Used only to anchor data for click listeners
                    val url: String,
                ): LinkPreview()

                data class TribeLinkPreview(
                    val name: TribePreviewName,
                    val description: PreviewDescription?,
                    val imageUrl: PreviewImageUrl?,
                    val showBanner: Boolean,

                    // Used only to anchor data for click listeners
                    val joinLink: TribeJoinLink,
                ) : LinkPreview()
            }

        }

        sealed class ContainerFourth private constructor(): Bubble() {

            data class Boost(
                val showSent: Boolean,
                val boostedByOwner: Boolean,
                val senders: Set<BoostSenderHolder>,
                private val totalAmount: Sat,
            ): ContainerFourth() {
                val amountText: String
                    get() = totalAmount.asFormattedString()

                val amountUnitLabel: String
                    get() = totalAmount.unit

                // will be gone if null is returned
                val numberUniqueBoosters: Int?
                    get() = if (senders.size > 1) {
                        senders.size
                    } else {
                        null
                    }
            }

            data class PaidMessageReceivedDetails(
                val amount: Sat,
                val purchaseStatus: PurchaseStatus,
                val showStatusIcon: Boolean,
                val showProcessingProgressBar: Boolean,
                val showStatusLabel: Boolean,
                val showPayElements: Boolean,
            ): ContainerFourth() {
                val amountText: String
                    get() = amount.asFormattedString(appendUnit = true)
            }
        }
    }
}

data class BoostSenderHolder(
    val photoUrl: PhotoUrl?,
    val alias: ContactAlias?,
    val colorKey: String,
)

data class ReplyUserHolder(
    val photoUrl: PhotoUrl?,
    val alias: ContactAlias?,
    val colorKey: String
)
