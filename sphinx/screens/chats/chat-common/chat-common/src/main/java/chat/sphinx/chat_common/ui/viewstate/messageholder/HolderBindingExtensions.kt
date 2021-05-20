package chat.sphinx.chat_common.ui.viewstate.messageholder

import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.core.view.updateLayoutParams
import chat.sphinx.chat_common.R
import chat.sphinx.resources.R as common_R
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_screens.util.visible

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setDirectPaymentLayout(
    directPayment: LayoutState.DirectPayment?
) {
    includeMessageHolderMessageTypes.includeMessageTypeDirectPayment.apply {
        if (directPayment == null) {
            root.gone
        } else {
            root.visible
            imageViewDirectPaymentSent.goneIfFalse(directPayment.showSent)
            imageViewDirectPaymentReceived.goneIfFalse(directPayment.showReceived)
            includeDirectPaymentAmountTextGroup.apply {
                textViewSatsAmount.text = directPayment.amount.asFormattedString()
                textViewSatsUnitLabel.text = directPayment.unitLabel
            }
        }
    }
}

@MainThread
internal fun LayoutMessageHolderBinding.setBackground(
    viewState: MessageHolderViewState,
    holderWidth: Px,
) {
    if (viewState.background is HolderBackground.None) {

        includeMessageHolderMessageTypes.root.gone
        val defaultMargins = root
            .context
            .resources
            .getDimensionPixelSize(common_R.dimen.default_layout_margin)

        receivedBubbleArrow.gone
        sentBubbleArrow.gone

        spaceMessageHolderLeft.updateLayoutParams { width = defaultMargins }
        spaceMessageHolderRight.updateLayoutParams { width = defaultMargins }

    } else {

        // TODO: Implement variable widths dependant on data
        val isIncoming: Boolean = when (viewState) {
            is MessageHolderViewState.InComing -> {
                spaceMessageHolderLeft.updateLayoutParams {
                    width = root
                        .context
                        .resources
                        .getDimensionPixelSize(R.dimen.message_holder_space_width_left)
                }
                spaceMessageHolderRight.updateLayoutParams {
                    width = (holderWidth.value * HolderBackground.SPACE_WIDTH_MULTIPLE).toInt()
                }

                true
            }
            is MessageHolderViewState.OutGoing -> {
                spaceMessageHolderLeft.updateLayoutParams {
                    width = (holderWidth.value * HolderBackground.SPACE_WIDTH_MULTIPLE).toInt()
                }
                spaceMessageHolderRight.updateLayoutParams {
                    width = root
                        .context
                        .resources
                        .getDimensionPixelSize(R.dimen.message_holder_space_width_right)
                }

                false
            }
        }

        receivedBubbleArrow.goneIfFalse(viewState.background is HolderBackground.First && isIncoming)
        sentBubbleArrow.goneIfFalse(viewState.background is HolderBackground.First && !isIncoming)

        includeMessageHolderMessageTypes.root.apply {
            visible

            // TODO: Figure out why this doesn't seem to clip the paid message details layout
            includeMessageHolderMessageTypes.root.clipToOutline = true

            @DrawableRes
            val resId: Int? = when (viewState.background) {
                HolderBackground.First.Grouped -> {
                    if (isIncoming) {
                        R.drawable.background_message_holder_in_first
                    } else {
                        R.drawable.background_message_holder_out_first
                    }
                }
                HolderBackground.First.Isolated,
                HolderBackground.Last -> {
                    if (isIncoming) {
                        R.drawable.background_message_holder_in_last
                    } else {
                        R.drawable.background_message_holder_out_last
                    }
                }
                HolderBackground.Middle -> {
                    if (isIncoming) {
                        R.drawable.background_message_holder_in_middle
                    } else {
                        R.drawable.background_message_holder_out_middle
                    }
                }
                HolderBackground.None -> {
                    /* will never make it here as this is already checked for */
                    null
                }
            }

            resId?.let { setBackgroundResource(it) }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setStatusHeader(
    statusHeader: LayoutState.MessageStatusHeader?
) {
    includeMessageStatusHeader.apply {
        if (statusHeader == null) {
            root.gone
        } else {
            root.visible

            textViewReceivedMessageSenderName.apply {
                statusHeader.senderName?.let { name ->
                    if (name.isEmpty()) {
                        gone
                    } else {
                        visible
                        text = name

                        /*
                        * TODO: Devise a way to derive random color values for sender aliases
                        *
                        * See the current iOS implementation: https://github.com/stakwork/sphinx/blob/9ee30302bc95091bcc9562e07ada87d52d27a5ad/sphinx/Scenes/Chat/Helpers/ChatHelper.swift#L12
                        *
                        * See current extension functions:
                        *   context.getRandomColor()
                        *   setBackgroundRandomColor()
                        * */
                        setTextColorExt(common_R.color.lightPurple)
                    }
                } ?: gone
            }

            layoutConstraintSentMessageContentContainer.goneIfFalse(statusHeader.isOutGoingMessage)
            layoutConstraintReceivedMessageContentContainer.goneIfFalse(statusHeader.isIncomingMessage)

            if (statusHeader.isOutGoingMessage) {
                textViewSentMessageTimestamp.text = statusHeader.timestamp
                textViewSentMessageBoltIcon.goneIfFalse(statusHeader.showBoltIcon)
                textViewSentMessageLockIcon.goneIfFalse(statusHeader.showLockIcon)
            } else {
                textViewReceivedMessageTimestamp.text = statusHeader.timestamp
                textViewReceivedMessageLockIcon.goneIfFalse(statusHeader.showLockIcon)
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setMessageTypeMessageLayout(
    messageContent: LayoutState.MessageTypeMessageContent?
) {
    includeMessageHolderMessageTypes.includeMessageTypeMessage.apply {
        if (messageContent == null) {
            root.gone
        } else {
            root.visible
            textViewMessageTypeMessage.text = messageContent.messageContent
        }
    }
}


@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setPaidMessageDetailsLayout(
    paidMessageDetailsContent: LayoutState.PaidMessageDetailsContent?
) {
    setPaidMessageSentStatusHeader(paidMessageDetailsContent)

    includeMessageHolderMessageTypes.includePaidMessageDetailsHolder.apply {
        if (
            paidMessageDetailsContent == null ||
            !paidMessageDetailsContent.isIncoming
        ) {
            root.gone
        } else {
            root.visible

            paidMessageDetailsContent.apply {
                imageViewPaymentReceivedIcon.goneIfTrue(isIncoming || isPaymentProcessing)
                imageViewSendPaymentIcon.goneIfFalse(isIncoming || isPaymentProcessing)
                textViewPaymentAcceptedIcon.goneIfFalse(isPaymentAccepted)
                progressWheelProcessingPayment.goneIfFalse(isPaymentProcessing)

                textViewPaymentStatusLabel.text = paymentStatusText
                textViewAmountToPayLabel.text = amountText
            }
        }
    }
}


@MainThread
@Suppress("NOTHING_TO_INLINE")
inline fun LayoutMessageHolderBinding.setPaidMessageSentStatusHeader(
    paidMessageDetailsContent: LayoutState.PaidMessageDetailsContent?
) {
    includeMessageHolderMessageTypes.includePaidMessageSentStatusDetails.apply {
        if (
            paidMessageDetailsContent == null ||
            !paidMessageDetailsContent.showSentMessageStatusHeader
        ) {
            root.gone
        } else {
            root.visible

            paidMessageDetailsContent.apply {
                attachmentPriceAmountLabel.text = paidMessageDetailsContent.amountText
                attachmentPriceStatusLabel.text = paidMessageDetailsContent.paymentStatusText
            }
        }
    }
}
