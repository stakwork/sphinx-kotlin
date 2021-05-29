package chat.sphinx.chat_common.ui.viewstate.messageholder

import android.view.Gravity
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.resources.R as common_R
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.resources.getString
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleDirectPaymentLayout(
    directPayment: LayoutState.Bubble.DirectPayment?
) {
    includeMessageHolderBubble.includeMessageTypeDirectPayment.apply {
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

// TODO: Refactor setting of spaces out of this extension function
@MainThread
internal fun LayoutMessageHolderBinding.setBubbleBackground(
    viewState: MessageHolderViewState,
    holderWidth: Px,
) {
    if (viewState.background is BubbleBackground.Gone) {

        includeMessageHolderBubble.root.gone
        receivedBubbleArrow.gone
        sentBubbleArrow.gone

    } else {
        receivedBubbleArrow.goneIfFalse(viewState.showReceivedBubbleArrow)
        sentBubbleArrow.goneIfFalse(viewState.showSentBubbleArrow)

        includeMessageHolderBubble.root.apply {
            visible

            @DrawableRes
            val resId: Int? = when (viewState.background) {
                BubbleBackground.First.Grouped -> {
                    if (viewState.isReceived) {
                        R.drawable.background_message_bubble_received_first
                    } else {
                        R.drawable.background_message_bubble_sent_first
                    }
                }
                BubbleBackground.First.Isolated,
                BubbleBackground.Last -> {
                    if (viewState.isReceived) {
                        R.drawable.background_message_bubble_received_last
                    } else {
                        R.drawable.background_message_bubble_sent_last
                    }
                }
                BubbleBackground.Middle -> {
                    if (viewState.isReceived) {
                        R.drawable.background_message_bubble_received_middle
                    } else {
                        R.drawable.background_message_bubble_sent_middle
                    }
                }
                is BubbleBackground.Gone -> {
                    /* will never make it here as this is already checked for */
                    null
                }
            }

            resId?.let { setBackgroundResource(it) }
        }
    }

    // Set background spacing
    if (viewState.background is BubbleBackground.Gone && viewState.background.setSpacingEqual) {

        val defaultMargins = root
            .context
            .resources
            .getDimensionPixelSize(common_R.dimen.default_layout_margin)

        spaceMessageHolderLeft.updateLayoutParams { width = defaultMargins }
        spaceMessageHolderRight.updateLayoutParams { width = defaultMargins }

    } else {

        @Exhaustive
        when (viewState) {
            is MessageHolderViewState.Received -> {
                spaceMessageHolderLeft.updateLayoutParams {
                    width = root
                        .context
                        .resources
                        .getDimensionPixelSize(R.dimen.message_holder_space_width_left)
                }
                spaceMessageHolderRight.updateLayoutParams {
                    width = (holderWidth.value * BubbleBackground.SPACE_WIDTH_MULTIPLE).toInt()
                }
            }
            is MessageHolderViewState.Sent -> {
                spaceMessageHolderLeft.updateLayoutParams {
                    width = (holderWidth.value * BubbleBackground.SPACE_WIDTH_MULTIPLE).toInt()
                }
                spaceMessageHolderRight.updateLayoutParams {
                    width = root
                        .context
                        .resources
                        .getDimensionPixelSize(R.dimen.message_holder_space_width_right)
                }
            }
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

            textViewMessageStatusReceivedSenderName.apply {
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

            layoutConstraintMessageStatusSentContainer.goneIfFalse(statusHeader.showSent)
            layoutConstraintMessageStatusReceivedContainer.goneIfFalse(statusHeader.showReceived)

            if (statusHeader.showSent) {
                textViewMessageStatusSentTimestamp.text = statusHeader.timestamp
                textViewMessageStatusSentBoltIcon.goneIfFalse(statusHeader.showBoltIcon)
                textViewMessageStatusSentLockIcon.goneIfFalse(statusHeader.showLockIcon)
            } else {
                textViewMessageStatusReceivedTimestamp.text = statusHeader.timestamp
                textViewMessageStatusReceivedLockIcon.goneIfFalse(statusHeader.showLockIcon)
            }
        }
    }
}


@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setDeletedMessageLayout(
    deletedMessage: LayoutState.DeletedMessage?
) {
    includeDeletedMessage.apply {
        if (deletedMessage == null) {
            root.gone
        } else {
            root.visible

            val gravity = if (deletedMessage.gravityStart) {
                Gravity.START
            } else {
                Gravity.END
            }

            textViewDeletedMessageTimestamp.text = deletedMessage.timestamp
            textViewDeletedMessageTimestamp.gravity = gravity
            textViewDeleteMessageLabel.gravity = gravity
        }
    }
}


@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleMessageLayout(
    message: LayoutState.Bubble.Message?
) {
    includeMessageHolderBubble.textViewMessageText.apply {
        if (message == null) {
            gone
        } else {
            visible
            text = message.text
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubblePaidMessageDetailsLayout(
    paidDetails: LayoutState.Bubble.PaidMessageDetails?
) {
    includeMessageHolderBubble.includePaidMessageReceivedDetailsHolder.apply {
        if (paidDetails == null) {
            root.gone
        } else {
            root.visible
            root.clipToOutline = true

            @ColorRes
            val backgroundTintResId = if (paidDetails.purchaseType is MessageType.Purchase.Denied) {
                R.color.badgeRed
            } else {
                R.color.primaryGreen
            }

            @DrawableRes
            val backgroundDrawableResId: Int? = when (paidDetails.bubbleBackground) {
                BubbleBackground.First.Grouped -> {
                    if (paidDetails.isShowingReceivedMessage) {
                        R.drawable.background_paid_message_details_bubble_footer_received_first
                    } else {
                        R.drawable.background_paid_message_details_bubble_footer_sent_first
                    }
                }
                BubbleBackground.First.Isolated,
                BubbleBackground.Last -> {
                    if (paidDetails.isShowingReceivedMessage) {
                        R.drawable.background_paid_message_details_bubble_footer_received_last
                    } else {
                        R.drawable.background_paid_message_details_bubble_footer_sent_last
                    }
                }
                BubbleBackground.Middle -> {
                    if (paidDetails.isShowingReceivedMessage) {
                        R.drawable.background_paid_message_details_bubble_footer_received_middle
                    } else {
                        R.drawable.background_paid_message_details_bubble_footer_sent_middle
                    }
                }
                else -> {
                    null
                }
            }

            @StringRes
            val statusTextResID = when (paidDetails.purchaseType) {
                MessageType.Purchase.Accepted -> {
                    R.string.purchase_status_label_paid_message_details_accepted
                }
                MessageType.Purchase.Denied -> {
                    R.string.purchase_status_label_paid_message_details_denied
                }
                MessageType.Purchase.Processing -> {
                    R.string.purchase_status_label_paid_message_details_processing
                }
                null -> {
                    R.string.purchase_status_label_paid_message_details_default
                }
            }

            backgroundDrawableResId?.let { root.setBackgroundResource(it) }
            root.backgroundTintList = ContextCompat.getColorStateList(root.context, backgroundTintResId)

            imageViewPaidMessageReceivedIcon.goneIfFalse(paidDetails.showPaymentReceivedIcon)
            imageViewPaidMessageSentIcon.goneIfFalse(paidDetails.showSendPaymentIcon)
            textViewPaymentAcceptedIcon.goneIfFalse(paidDetails.showPaymentAcceptedIcon)
            progressBarPaidMessage.goneIfFalse(paidDetails.showPaymentProgressWheel)
            textViewPaidMessageStatusLabel.text = getString(statusTextResID)
            textViewPaidMessageAmountToPayLabel.text = paidDetails.amountText
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubblePaidMessageSentStatusLayout(
    paidSentStatus: LayoutState.Bubble.PaidMessageSentStatus?
) {
    includeMessageHolderBubble.includePaidMessageSentStatusDetails.apply {
        if (paidSentStatus == null) {
            root.gone
        } else {
            root.visible

            val statusTextResID = when (paidSentStatus.purchaseType) {
                MessageType.Purchase.Accepted -> {
                    R.string.purchase_status_label_paid_message_sent_status_accepted
                }
                MessageType.Purchase.Denied -> {
                    R.string.purchase_status_label_paid_message_sent_status_denied
                }
                MessageType.Purchase.Processing -> {
                    R.string.purchase_status_label_paid_message_sent_status_processing
                }
                null -> {
                    R.string.purchase_status_label_paid_message_sent_status_default
                }
            }

            textViewPaidMessageSentStatusAmount.text = paidSentStatus.amountText
            textViewPaidMessageSentStatus.text = getString(statusTextResID)
        }
    }
}
