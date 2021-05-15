package chat.sphinx.chat_common.ui.viewstate.messageholder

import androidx.annotation.MainThread
import app.cash.exhaustive.Exhaustive
import chat.sphinx.resources.R
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.isReceived
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_screens.util.visible

@MainThread
@Suppress("NOTHING_TO_INLINE")
inline fun LayoutMessageHolderBinding.setDirectPaymentLayout(directPayment: LayoutState.DirectPayment?) {
    includeMessageHolderMessageTypes.includeMessageTypeDirectPayment.apply {
        if (directPayment == null) {
            root.gone
        } else {
            root.visible
            imageViewDirectPaymentSent.goneIfFalse(directPayment.showSent)
            imageViewDirectPaymentReceived.goneIfFalse(directPayment.showReceived)
            includeDirectPaymentAmountTextGroup.apply {
                textViewBoostAmount.text = directPayment.amount.asFormattedString()
                textViewBoostUnitLabel.text = directPayment.unitLabel
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
inline fun LayoutMessageHolderBinding.setHeaderStatus(
    background: HolderBackground,
    message: Message,
    chatType: ChatType?,
) {
    @Exhaustive
    when (background) {
        is HolderBackground.None -> {
            includeMessageStatusHeader.root.gone
        }
        is HolderBackground.In -> {
            includeMessageStatusHeader.apply {

                @Exhaustive
                when (background) {
                    is HolderBackground.In.First -> {
                        root.visible
                    }
                    is HolderBackground.In.Middle -> {
                        // TODO: Hide header on all but the `FIRST` message in a group
//                        root.gone
                        root.visible
                    }
                    is HolderBackground.In.Last -> {
                        // TODO: Hide header on all but the `FIRST` message in a group
//                        root.gone
                        root.visible
                    }
                }

                layoutConstraintReceivedMessageContentContainer.visible
                layoutConstraintSentMessageContentContainer.gone

                if (chatType?.isConversation() == true) {
                    textViewReceivedMessageSenderName.gone
                } else {
                    textViewReceivedMessageSenderName.apply {
                        visible
                        text = message.senderAlias?.value ?: ""

                        /*
                        * TODO: Devise a way to derive random color values for sender aliases
                        *
                        * See the current iOS implementation: https://github.com/stakwork/sphinx/blob/9ee30302bc95091bcc9562e07ada87d52d27a5ad/sphinx/Scenes/Chat/Helpers/ChatHelper.swift#L12
                        *
                        * See current extension functions:
                        *   context.getRandomColor()
                        *   setBackgroundRandomColor()
                        * */
                        setTextColorExt(R.color.lightPurple)
                    }
                }

                textViewReceivedMessageLockIcon.goneIfTrue(message.messageContentDecrypted == null)
                textViewReceivedMessageTimestamp.text = DateTime.getFormathmma().format(message.date.value)
            }
        }
        is HolderBackground.Out -> {
            includeMessageStatusHeader.apply {

                @Exhaustive
                when (background) {
                    is HolderBackground.Out.First -> {
                        root.visible
                    }
                    is HolderBackground.Out.Last -> {
                        // TODO: Hide header on all but the `FIRST` message in a group
//                        root.gone
                        root.visible
                    }
                    is HolderBackground.Out.Middle -> {
                        // TODO: Hide header on all but the `FIRST` message in a group
//                        root.gone
                        root.visible
                    }
                }

                layoutConstraintReceivedMessageContentContainer.gone
                layoutConstraintSentMessageContentContainer.visible

                textViewSentMessageLockIcon.goneIfTrue(message.messageContentDecrypted == null)
                textViewSentMessageBoltIcon.goneIfFalse(message.status.isReceived())
                textViewSentMessageTimestamp.text = DateTime.getFormathmma().format(message.date.value)
            }
        }
    }
}