package chat.sphinx.chat_common.ui.viewstate.messageholder

import android.view.Gravity
import android.widget.ImageView
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
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible


@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setUnsupportedMessageTypeLayout(
    messageTypeDetails: LayoutState.UnsupportedMessageType?
) {
    includeMessageHolderBubble.includeUnsupportedMessageTypePlaceholder.apply {
        if (messageTypeDetails == null) {
            root.gone
        } else {
            root.visible

            val messageTypeDisplayString = when (messageTypeDetails.messageType) {
                MessageType.Delete,
                MessageType.DirectPayment,
                MessageType.GroupAction.Join,
                MessageType.GroupAction.Leave,
                MessageType.Message -> {
                    // 🤔 We should never get here since these message types ARE supported.
                    getString(R.string.placeholder_unsupported_message_type_default)
                }
                MessageType.Attachment -> {
                    getString(R.string.placeholder_display_name_message_type_attachment)
                }
                MessageType.BotRes -> {
                    getString(R.string.placeholder_display_name_message_type_bot_response)
                }
                MessageType.Invoice -> {
                    getString(R.string.placeholder_display_name_message_type_invoice)
                }
                MessageType.Payment -> {
                    getString(R.string.placeholder_display_name_message_type_payment)
                }
                MessageType.GroupAction.TribeDelete -> {
                    getString(R.string.placeholder_display_name_message_type_tribe_delete)
                }
                MessageType.Cancellation,
                MessageType.Confirmation,
                MessageType.ContactKey,
                MessageType.ContactKeyConfirmation,
                MessageType.GroupAction.Create,
                MessageType.GroupAction.Invite,
                MessageType.GroupAction.Kick,
                MessageType.Heartbeat,
                MessageType.HeartbeatConfirmation,
                MessageType.KeySend,
                MessageType.Purchase.Accepted,
                MessageType.Purchase.Denied,
                MessageType.Purchase.Processing,
                MessageType.GroupAction.MemberApprove,
                MessageType.GroupAction.MemberReject,
                MessageType.GroupAction.MemberRequest,
                MessageType.Query,
                MessageType.QueryResponse,
                MessageType.Repayment,
                MessageType.Boost,
                MessageType.BotCmd,
                MessageType.BotInstall,
                is MessageType.Unknown -> {
                    // ❓: Should we ever get here if the type isn't one we plan to
                    // render a unique message for?
                    getString(R.string.placeholder_unsupported_message_type_default)
                }
            }

            textViewPlaceholderMessage.text = root.context.getString(
                R.string.unsupported_message_type_placeholder_text,
                messageTypeDisplayString
            )
            textViewPlaceholderMessage.gravity = if (messageTypeDetails.gravityStart) Gravity.START else Gravity.END
        }
    }
}



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
    paidDetails: LayoutState.Bubble.PaidMessageDetails?,
    bubbleBackground: BubbleBackground
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
            val backgroundDrawableResId: Int? = when (bubbleBackground) {
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

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleGiphy(
    message: LayoutState.Bubble.ContainerBottom.Giphy?,
    loadImage: (ImageView, GiphyUrl) -> Unit,
) {
    includeMessageHolderBubble.includeMessageTypeImageAttachment.apply {
        if ((message?.message?.giphyData?.url?.isNotEmpty() == true)) {
            message?.giphyUrl?.let { giphyUrl ->
                imageViewAttachmentImage.visible
                loadImage(imageViewAttachmentImage, giphyUrl)
            }
        } else {
            imageViewAttachmentImage.gone
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleReactionBoosts(
    boost: LayoutState.Bubble.ContainerBottom.Boost?,
    loadImage: (ImageView, SenderPhotoUrl) -> Unit,
) {
    includeMessageHolderBubble.includeMessageTypeBoost.apply {
        if (boost == null) {
            root.gone
        } else {
            root.visible

//            imageViewBoostMessageIcon
            includeBoostAmountTextGroup.apply {
                textViewSatsAmount.text = boost.amountText
                textViewSatsUnitLabel.text = boost.amountUnitLabel
            }

            includeBoostReactionsGroup.apply {

                includeBoostReactionImageHolder1.apply {
                    boost.senderPics.elementAtOrNull(0).let { holder ->
                        if (holder == null) {
                            root.gone
                        } else {
                            root.visible

                            @Exhaustive
                            when (holder) {
                                is SenderInitials -> {
                                    textViewInitials.visible
                                    textViewInitials.text = holder.value
                                    textViewInitials.setBackgroundRandomColor(R.drawable.chat_initials_circle)
                                    imageViewChatPicture.gone
                                }
                                is SenderPhotoUrl -> {
                                    textViewInitials.gone
                                    imageViewChatPicture.visible
                                    loadImage(imageViewChatPicture, holder)
                                }
                            }
                        }
                    }
                }

                includeBoostReactionImageHolder2.apply {
                    boost.senderPics.elementAtOrNull(1).let { holder ->
                        if (holder == null) {
                            root.gone
                        } else {
                            root.visible

                            @Exhaustive
                            when (holder) {
                                is SenderInitials -> {
                                    textViewInitials.visible
                                    textViewInitials.text = holder.value
                                    textViewInitials.setBackgroundRandomColor(R.drawable.chat_initials_circle)
                                    imageViewChatPicture.gone
                                }
                                is SenderPhotoUrl -> {
                                    textViewInitials.gone
                                    imageViewChatPicture.visible
                                    loadImage(imageViewChatPicture, holder)
                                }
                            }
                        }
                    }
                }

                includeBoostReactionImageHolder3.apply {
                    boost.senderPics.elementAtOrNull(2).let { holder ->
                        if (holder == null) {
                            root.gone
                        } else {
                            root.visible

                            @Exhaustive
                            when (holder) {
                                is SenderInitials -> {
                                    textViewInitials.visible
                                    textViewInitials.text = holder.value
                                    textViewInitials.setBackgroundRandomColor(R.drawable.chat_initials_circle)
                                    imageViewChatPicture.gone
                                }
                                is SenderPhotoUrl -> {
                                    textViewInitials.gone
                                    imageViewChatPicture.visible
                                    loadImage(imageViewChatPicture, holder)
                                }
                            }
                        }
                    }
                }

                textViewBoostReactionCount.apply {
                    boost.numberUniqueBoosters?.let { count ->
                        visible
                        text = count.toString()
                    } ?: gone
                }
            }
        }
    }
}


@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setGroupActionIndicatorLayout(
    groupActionDetails: LayoutState.GroupActionIndicator?
) {
    if (groupActionDetails == null) {
        includeMessageTypeGroupActionHolder.root.gone
    } else {
        includeMessageTypeGroupActionHolder.root.visible

        when (groupActionDetails.actionType) {
            MessageType.GroupAction.Join,
            MessageType.GroupAction.Leave -> {
                setGroupActionAnnouncementLayout(groupActionDetails)
            }
            MessageType.GroupAction.Kick,
            MessageType.GroupAction.TribeDelete -> {
                setGroupActionMemberRemovalLayout(groupActionDetails)
            }
            MessageType.GroupAction.MemberApprove -> {
                if (groupActionDetails.isAdminView) {
                    setGroupActionJoinRequestAdminLayout(groupActionDetails)
                } else {
                    setGroupActionAnnouncementLayout(groupActionDetails)
                }
            }
            MessageType.GroupAction.MemberReject -> {
                if (groupActionDetails.isAdminView) {
                    setGroupActionJoinRequestAdminLayout(groupActionDetails)
                } else {
                    setGroupActionMemberRemovalLayout(groupActionDetails)
                }
            }
            MessageType.GroupAction.MemberRequest -> {
                if (groupActionDetails.isAdminView) {
                    setGroupActionJoinRequestAdminLayout(groupActionDetails)
                } else {
                    includeMessageTypeGroupActionHolder.root.gone
                }
            }
            else -> {
                // If we get here, it's an action that we don't have layouts
                // designed for yet.
                includeMessageTypeGroupActionHolder.root.gone
            }
        }
    }
}


/**
 * Announces the result of a group action.
 *
 * When this is the result of handling a join request, it will
 * either tell an admin what they did, or tell a user what an admin did to them.
 */
@MainThread
@Suppress("NOTHING_TO_INLINE")
private inline fun LayoutMessageHolderBinding.setGroupActionAnnouncementLayout(
    groupActionDetails: LayoutState.GroupActionIndicator
) {
    includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionAnnouncement.apply {
        root.visible

        val actionLabelText = when (groupActionDetails.actionType) {
            MessageType.GroupAction.Join -> {
                if (groupActionDetails.chatType == ChatType.Tribe) {
                    root.context.getString(R.string.group_join_announcement_tribe, groupActionDetails.subjectName)
                } else {
                    root.context.getString(R.string.group_join_announcement_group, groupActionDetails.subjectName)
                }
            }
            MessageType.GroupAction.Leave -> {
                if (groupActionDetails.chatType == ChatType.Tribe) {
                    root.context.getString(R.string.group_leave_announcement_tribe, groupActionDetails.subjectName)
                } else {
                    root.context.getString(R.string.group_leave_announcement_group, groupActionDetails.subjectName)
                }
            }
            else -> {
                null
            }
        }

        actionLabelText?.let {
            textViewGroupActionLabel.text = it
        } ?: root.gone
    }
}

/**
 * Presents a view for an admin to handle a group membership request
 */
@MainThread
@Suppress("NOTHING_TO_INLINE")
private inline fun LayoutMessageHolderBinding.setGroupActionJoinRequestAdminLayout(
    groupActionDetails: LayoutState.GroupActionIndicator
) {
    includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionJoinRequestAdminView.apply {
        root.visible

        // TODO: Set text and wire up action button click handlers.
    }
}


/**
 * Tells a member that they've been removed from a group and shows
 * a button that lets them delete the group.
 */
@MainThread
@Suppress("NOTHING_TO_INLINE")
private inline fun LayoutMessageHolderBinding.setGroupActionMemberRemovalLayout(
    groupActionDetails: LayoutState.GroupActionIndicator
) {
    includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionMemberRemoval.apply {
        root.visible

        // TODO: Set text and wire up action button click handler.
    }
}


@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleReplyMessage(
    replyMessage: LayoutState.Bubble.ReplyMessage?
) {
    includeMessageHolderBubble.includeMessageReply.apply {
        if (replyMessage == null) {
            root.gone
        } else {
            root.visible

            imageViewReplyMediaImage.apply {
//                @Exhaustive
//                when (replyMessage.media) {
//                    is MediaUrl -> {
//                        visible
//                    }
//                    is MediaFile -> {
//                        visible
//                    }
//                    null -> {
//                        gone
//                    }
//                }
                // TODO: handle attachment types and make visible
                gone
            }
            imageViewReplyTextOverlay.gone

            // Only used in the footer when replying to a message
            textViewReplyClose.gone

            textViewReplyMessageLabel.text = replyMessage.text
            textViewReplySenderLabel.text = replyMessage.sender
            viewReplyBarLeading.setBackgroundRandomColor(null)
        }
    }
}

