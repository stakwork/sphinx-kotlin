package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_message_media.MediaKey
import chat.sphinx.wrapper_message_media.MessageMedia

internal sealed class LayoutState {

    data class MessageStatusHeader(
        val senderName: String?,
        val showSent: Boolean,

        // TODO: rework bolt icon when sending messages to be yellow (sending), red (failed), green(sent)
        val showBoltIcon: Boolean,

        val showLockIcon: Boolean,
        val timestamp: String,
    ): LayoutState() {
        val showReceived: Boolean
            get() = !showSent
    }


    data class GroupActionIndicator(
        val actionType: MessageType.GroupAction,
        val chatType: ChatType?,
        val isAdminView: Boolean,
        val subjectName: String?,
    ): LayoutState()


    data class DeletedMessage(
        val gravityStart: Boolean,
        val timestamp: String,
    ): LayoutState()

    sealed class Bubble: LayoutState() {

        sealed class ContainerTop: Bubble() {

            data class PaidMessageSentStatus(
                val amount: Sat,
                val purchaseType: MessageType.Purchase?,
            ): ContainerTop() {
                val amountText: String
                    get() = amount.asFormattedString(appendUnit = true)
            }

            data class DirectPayment(
                val showSent: Boolean,
                val amount: Sat
            ): ContainerTop() {
                val showReceived: Boolean
                    get() = !showSent

                val unitLabel: String
                    get() = amount.unit
            }

            data class ImageAttachment(
                val url: String,
                val media: MessageMedia?,
            ): ContainerTop()

            // FileAttachment
            // AudioAttachment
            // VideoAttachment

            data class ReplyMessage(
                // TODO: Make sealed interface for handling a url or file
//            val media: String?,
                val sender: String,
                val text: String,
            ): ContainerTop()

            // CallInvite
            // Invoice
        }

        sealed class ContainerMiddle: Bubble() {

            data class UnsupportedMessageType(
                val messageType: MessageType,
                val gravityStart: Boolean,
            ): ContainerMiddle()

            data class Message(
                val text: String
            ): ContainerMiddle()

            // MessageLinkPreview
            // TribeLinkPreview
            // UrlLinkPreview

        }

        sealed class ContainerBottom: Bubble() {

            data class Boost(
                private val totalAmount: Sat,
                val senderPics: Set<BoostReactionImageHolder>
            ): ContainerBottom() {
                val amountText: String
                    get() = totalAmount.asFormattedString()

                val amountUnitLabel: String
                    get() = totalAmount.unit

                // will be gone if null is returned
                val numberUniqueBoosters: Int?
                    get() = if (senderPics.size > 1) {
                        senderPics.size
                    } else {
                        null
                    }
            }

            data class PaidMessageDetails(
                val amount: Sat,
                val purchaseType: MessageType.Purchase?,
                val isShowingReceivedMessage: Boolean,
                val showPaymentAcceptedIcon: Boolean,
                val showPaymentProgressWheel: Boolean,
                val showSendPaymentIcon: Boolean,
                val showPaymentReceivedIcon: Boolean,
            ): ContainerBottom() {
                val amountText: String
                    get() = amount.asFormattedString(appendUnit = true)
            }
        }
    }
}

// TODO: TEMPORARY!!! until Initial holder can be refactored...

@JvmInline
value class SenderPhotoUrl(val value: String): BoostReactionImageHolder

@JvmInline
value class SenderInitials(val value: String): BoostReactionImageHolder

sealed interface BoostReactionImageHolder
