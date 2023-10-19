package chat.sphinx.chat_common.ui.viewstate.messageholder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.adapters.MessageListAdapter
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.chat_common.databinding.LayoutMessageTypeAttachmentAudioBinding
import chat.sphinx.chat_common.databinding.LayoutMessageTypePodcastClipBinding
import chat.sphinx.chat_common.model.FeedItemPreview
import chat.sphinx.chat_common.model.NodeDescriptor
import chat.sphinx.chat_common.model.TribeLink
import chat.sphinx.chat_common.model.UnspecifiedUrl
import chat.sphinx.chat_common.ui.viewstate.audio.AudioMessageState
import chat.sphinx.chat_common.ui.viewstate.audio.AudioPlayState
import chat.sphinx.chat_common.util.AudioPlayerController
import chat.sphinx.chat_common.util.SphinxLinkify
import chat.sphinx.chat_common.util.SphinxUrlSpan
import chat.sphinx.chat_common.util.VideoThumbnailUtil
import chat.sphinx.concept_image_loader.*
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_client_crypto.CryptoHeader
import chat.sphinx.concept_network_client_crypto.CryptoScheme
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.resources.*
import chat.sphinx.resources.databinding.LayoutChatImageSmallInitialHolderBinding
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.asFormattedString
import chat.sphinx.wrapper_common.before
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.util.getHHMMSSString
import chat.sphinx.wrapper_common.util.getHHMMString
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_meme_server.headerKey
import chat.sphinx.wrapper_meme_server.headerValue
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.MessageMedia
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.*
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import chat.sphinx.resources.R as common_R


@MainThread
@Suppress("NOTHING_TO_INLINE")
internal fun  LayoutMessageHolderBinding.setView(
    lifecycleScope: CoroutineScope,
    holderJobs: ArrayList<Job>,
    disposables: ArrayList<Disposable>,
    dispatchers: CoroutineDispatchers,
    audioPlayerController: AudioPlayerController,
    imageLoader: ImageLoader<ImageView>,
    memeServerTokenHandler: MemeServerTokenHandler,
    recyclerViewWidth: Px,
    viewState: MessageHolderViewState,
    userColorsHelper: UserColorsHelper,
    onSphinxInteractionListener: SphinxUrlSpan.OnInteractionListener? = null,
    onRowLayoutListener: MessageListAdapter.OnRowLayoutListener? = null,
) {

    for (job in holderJobs) {
        job.cancel()
    }
    holderJobs.clear()

    for (disposable in disposables) {
        disposable.dispose()
    }
    disposables.clear()

    fun loadImageAttachment(
        imageView: ImageView,
        loadingContainer: ConstraintLayout,
        url: String,
        media: MessageMedia?
    ) {
        lifecycleScope.launch(dispatchers.mainImmediate) {
            val file: File? = media?.localFile

            val options: ImageLoaderOptions? = if (media != null) {
                val builder = ImageLoaderOptions.Builder()

                builder.errorResId(
                    if (viewState is MessageHolderViewState.Sent) {
                        R.drawable.sent_image_not_available
                    } else {
                        R.drawable.received_image_not_available
                    }
                )

                if (file == null) {
                    media.host?.let { host ->
                        memeServerTokenHandler.retrieveAuthenticationToken(host)
                            ?.let { token ->
                                builder.addHeader(token.headerKey, token.headerValue)

                                media.mediaKeyDecrypted?.value?.let { key ->
                                    val header = CryptoHeader.Decrypt.Builder()
                                        .setScheme(CryptoScheme.Decrypt.JNCryptor)
                                        .setPassword(key)
                                        .build()

                                    builder.addHeader(header.key, header.value)
                                }
                            }
                    }
                }

                builder.build()
            } else {
                null
            }

            val disposable: Disposable = if (file != null) {
                imageLoader.load(imageView, file, options, object : OnImageLoadListener {
                    override fun onSuccess() {
                        super.onSuccess()
                        loadingContainer.gone
                        onRowLayoutListener?.onRowHeightChanged()
                    }
                })
            } else {
                imageLoader.load(imageView, url, options, object : OnImageLoadListener {
                    override fun onSuccess() {
                        super.onSuccess()
                        loadingContainer.gone
                        onRowLayoutListener?.onRowHeightChanged()
                    }
                })
            }

            disposables.add(disposable)
            disposable.await()
        }
    }


    apply {
        lifecycleScope.launch(dispatchers.mainImmediate) {
            val initialsColor = viewState.statusHeader?.colorKey?.let { key ->
                Color.parseColor(
                    userColorsHelper.getHexCodeForKey(key, root.context.getRandomHexCode())
                )
            }

            viewState.initialHolder.setInitialHolder(
                includeMessageHolderChatImageInitialHolder.textViewInitialsName,
                includeMessageHolderChatImageInitialHolder.imageViewChatPicture,
                includeMessageStatusHeader,
                imageLoader,
                initialsColor
            )?.also {
                disposables.add(it)
            }
        }.let { job ->
            holderJobs.add(job)
        }
        setSearchHighlightedStatus(
            viewState.searchHighlightedStatus
        )
        setMessagesSeparator(
            viewState.messagesSeparator
        )
        setStatusHeader(
            viewState.statusHeader,
            holderJobs,
            dispatchers,
            lifecycleScope,
            userColorsHelper,
        )
        setInvoiceExpirationHeader(
            viewState.invoiceExpirationHeader
        )
        setBubbleBackground(
            viewState, recyclerViewWidth
        )
        setDeletedOrFlaggedMessageLayout(
            viewState.deletedOrFlaggedMessage
        )
        setInvoicePaymentLayout(
            viewState.invoicePayment
        )
        setInvoiceDottedLinesLayout(
            viewState
        )
        setGroupActionIndicatorLayout(
            viewState.groupActionIndicator
        )
        if (viewState.background !is BubbleBackground.Gone) {
            setBubbleImageAttachment(viewState.bubbleImageAttachment) { imageView, loadingContainer, url, media ->
                loadImageAttachment(imageView,loadingContainer, url, media)
            }

            setBubbleAudioAttachment(
                viewState.bubbleAudioAttachment,
                audioPlayerController,
                dispatchers,
                holderJobs,
                lifecycleScope
            )
            setBubblePodcastClip(
                viewState.bubblePodcastClip,
                audioPlayerController,
                dispatchers,
                holderJobs,
                lifecycleScope
            )
            setBubbleVideoAttachment(
                viewState.bubbleVideoAttachment,
            )

            setBubbleFileAttachment(
                viewState.bubbleFileAttachment
            )
            setUnsupportedMessageTypeLayout(
                viewState.unsupportedMessageType
            )
            setBubbleMessageLayout(
                viewState.bubbleMessage,
                onSphinxInteractionListener
            )
            setBubbleThreadLayout(
                viewState.bubbleThread,
                holderJobs,
                dispatchers,
                lifecycleScope,
                userColorsHelper,
                audioPlayerController,
                loadImage = { imageView, url,  ->
                lifecycleScope.launch(dispatchers.mainImmediate) {
                    imageLoader.load(
                        imageView,
                        url,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(R.drawable.ic_profile_avatar_circle)
                            .transformation(Transformation.CircleCrop)
                            .build()
                    )
                        .also { disposables.add(it) }
                }.let { job ->
                    holderJobs.add(job)
                }
            }, lastReplyImage = { imageView, constraintLayout, url, media ->
                    loadImageAttachment(imageView, constraintLayout, url, media)
            })
            setBubblePaidMessageLayout(
                dispatchers,
                holderJobs,
                lifecycleScope,
                viewState,
                onSphinxInteractionListener
            )
            setBubbleMessageLinkPreviewLayout(
                dispatchers,
                holderJobs,
                imageLoader,
                lifecycleScope,
                viewState,
                onRowLayoutListener,
            )
            setBubbleCallInvite(
                viewState.bubbleCallInvite
            )
            setBubbleBotResponse(
                viewState.bubbleBotResponse,
                onRowLayoutListener
            )
            setBubbleDirectPaymentLayout(
                viewState.bubbleDirectPayment,
                holderJobs,
                dispatchers,
                lifecycleScope,
                userColorsHelper
            ) { imageView, url ->
                lifecycleScope.launch(dispatchers.mainImmediate) {

                    val disposable: Disposable = imageLoader.load(
                        imageView,
                        url,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(R.drawable.ic_profile_avatar_circle)
                            .transformation(Transformation.CircleCrop)
                            .build()
                    )

                    disposables.add(disposable)
                    disposable.await()
                }.let { job ->
                    holderJobs.add(job)
                }
            }
            setBubbleInvoiceLayout(
                viewState.bubbleInvoice
            )
            setBubblePodcastBoost(
                viewState.bubblePodcastBoost
            )
            setBubblePaidMessageReceivedDetailsLayout(
                viewState.bubblePaidMessageReceivedDetails,
                viewState.background
            )
            setBubblePaidMessageSentStatusLayout(
                viewState.bubblePaidMessageSentStatus
            )
            setBubbleReactionBoosts(
                viewState.bubbleReactionBoosts,
                holderJobs,
                dispatchers,
                lifecycleScope,
                userColorsHelper
            ) { imageView, url ->
                lifecycleScope.launch(dispatchers.mainImmediate) {
                    imageLoader.load(
                        imageView, 
                        url,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(R.drawable.ic_profile_avatar_circle)
                            .transformation(Transformation.CircleCrop)
                            .build()    
                    )
                        .also { disposables.add(it) }
                }.let { job ->
                    holderJobs.add(job)
                }
            }
            setBubbleReplyMessage(
                viewState.bubbleReplyMessage,
                holderJobs,
                dispatchers,
                lifecycleScope,
                userColorsHelper,
            ) { imageView, url, media ->
                lifecycleScope.launch(dispatchers.mainImmediate) {

                    val file: File? = media?.localFile

                    val options: ImageLoaderOptions? = if (media != null) {
                        val builder = ImageLoaderOptions.Builder()

                        builder.errorResId(
                            if (viewState is MessageHolderViewState.Sent) {
                                R.drawable.sent_image_not_available
                            } else {
                                R.drawable.received_image_not_available
                            }
                        )

                        if (file == null) {
                            media.host?.let { host ->
                                memeServerTokenHandler.retrieveAuthenticationToken(host)
                                    ?.let { token ->
                                        builder.addHeader(token.headerKey, token.headerValue)

                                        media.mediaKeyDecrypted?.value?.let { key ->
                                            val header = CryptoHeader.Decrypt.Builder()
                                                .setScheme(CryptoScheme.Decrypt.JNCryptor)
                                                .setPassword(key)
                                                .build()

                                            builder.addHeader(header.key, header.value)
                                        }
                                    }
                            }
                        }

                        builder.build()
                    } else {
                        null
                    }

                    val disposable: Disposable = if (file != null) {
                        imageLoader.load(imageView, file, options)
                    } else {
                        imageLoader.load(imageView, url, options)
                    }

                    disposables.add(disposable)
                    disposable.await()
                }.let { job ->
                    holderJobs.add(job)
                }
            }
        }
    }

}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setUnsupportedMessageTypeLayout(
    unsupportedMessage: LayoutState.Bubble.ContainerThird.UnsupportedMessageType?
) {
    includeMessageHolderBubble.includeUnsupportedMessageTypePlaceholder.apply {
        if (unsupportedMessage == null) {
            root.gone
        } else {
            root.visible

            val messageTypeDisplayString = when (unsupportedMessage.messageType) {
                MessageType.Delete,
                MessageType.DirectPayment,
                MessageType.GroupAction.Join,
                MessageType.GroupAction.Leave,
                MessageType.CallLink,
                MessageType.Message -> {
                    // 🤔 We should never get here since these message types ARE supported.
                    getString(R.string.placeholder_unsupported_message_type_default)
                }
                MessageType.Attachment -> {
                    getString(R.string.placeholder_display_name_message_type_attachment)
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
                MessageType.BotRes,
                MessageType.BotCmd,
                MessageType.BotInstall,
                is MessageType.Unknown -> {
                    getString(R.string.placeholder_unsupported_message_type_default)
                }
            }

            textViewPlaceholderMessage.text = root.context.getString(
                R.string.unsupported_message_type_placeholder_text,
                messageTypeDisplayString
            )
            textViewPlaceholderMessage.gravity = if (unsupportedMessage.gravityStart) Gravity.START else Gravity.END
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleDirectPaymentLayout(
    directPayment: LayoutState.Bubble.ContainerSecond.DirectPayment?,
    holderJobs: ArrayList<Job>,
    dispatchers: CoroutineDispatchers,
    lifecycleScope: CoroutineScope,
    userColorsHelper: UserColorsHelper,
    loadImage: (ImageView, String) -> Unit
) {
    includeMessageHolderBubble.includeMessageTypeDirectPayment.apply {
        if (directPayment == null) {
            root.gone
        } else {
            root.visible

            layoutConstraintDirectPaymentRecipient.goneIfFalse(directPayment.isTribe)

            layoutConstraintTribeDirectPaymentReceivedContainer.goneIfFalse(directPayment.showReceived && directPayment.isTribe)
            layoutConstraintDirectPaymentReceivedContainer.goneIfFalse(directPayment.showReceived && !directPayment.isTribe)
            layoutConstraintDirectPaymentSentContainer.goneIfFalse(directPayment.showSent)

            textViewTribeSatsAmountReceived.text = directPayment.amount.asFormattedString()

            textViewSatsAmountReceived.text = directPayment.amount.asFormattedString()
            textViewSatsUnitLabelReceived.text = directPayment.unitLabel

            textViewSatsAmountSent.text = directPayment.amount.asFormattedString()
            textViewSatsUnitLabelSent.text = directPayment.unitLabel

            if (directPayment.isTribe) {
                imageViewRecipientPicture.gone

                lifecycleScope.launch(dispatchers.mainImmediate) {
                    val initialsColor = Color.parseColor(
                        userColorsHelper.getHexCodeForKey(directPayment.recipientColorKey, root.context.getRandomHexCode())
                    )

                    textViewRecipientInitials.text = (directPayment.recipientAlias?.value ?: getString(R.string.unknown)).getInitials()
                    textViewRecipientInitials.setInitialsColor(
                        initialsColor,
                        R.drawable.chat_initials_circle
                    )
                }.let { job ->
                    holderJobs.add(job)
                }

                directPayment.recipientPic?.let { recipientPic ->
                    imageViewRecipientPicture.visible

                    loadImage(imageViewRecipientPicture, recipientPic.value)
                }
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleInvoiceLayout(
    invoice: LayoutState.Bubble.ContainerSecond.Invoice?
) {
    includeMessageHolderBubble.includeMessageTypeInvoice.apply {
        if (invoice == null) {
            root.gone

            includeMessageInvoiceDottedLinesHolder.apply {
                viewInvoiceBottomLeftLine.gone
                viewInvoiceBottomRightLine.gone
            }
        } else {
            root.visible

            includeMessageInvoiceDottedLinesHolder.apply {
                viewInvoiceBottomLeftLine.goneIfFalse(invoice.showReceived && invoice.showPaidInvoiceBottomLine)
                viewInvoiceBottomRightLine.goneIfFalse(invoice.showSent && invoice.showPaidInvoiceBottomLine)
            }

            //Pending invoices shows with no bubble but dashed border line. Arrows can't be hide
            //since status header is visible and they are needed for constraints
            if (invoice.hideBubbleArrows) {
                receivedBubbleArrow.visibility = View.INVISIBLE
                sentBubbleArrow.visibility = View.INVISIBLE
            }

            layoutConstraintPayButtonContainer.goneIfFalse(invoice.showPayButton)
            layoutConstraintInvoiceDashedBorder.goneIfFalse(invoice.showDashedBorder)

            viewInvoiceDashedBorder.background = AppCompatResources.getDrawable(
                root.context,
                if (invoice.showReceived) R.drawable.background_received_pending_invoice else R.drawable.background_sent_pending_invoice
            )

            imageViewQrIconLeading.setImageDrawable(
                AppCompatResources.getDrawable(root.context,
                    if (invoice.showExpiredLayout) {
                        R.drawable.qr_code_error
                    } else {
                        R.drawable.ic_qr_code
                    }
                )
            )

            textViewInvoiceAmountNumber.text = invoice.amount.asFormattedString()
            textViewInvoiceAmountUnit.text = invoice.unitLabel

            textViewInvoiceMessage.text = invoice.text
            textViewInvoiceMessage.goneIfFalse(invoice.text.isNotEmpty())

            val amountAndUnitColor = ContextCompat.getColor(root.context,
                if (invoice.showExpiredLayout) {
                    if (invoice.showReceived) R.color.washedOutReceivedText else R.color.washedOutSentText
                } else {
                    R.color.text
                }
            )

            textViewInvoiceAmountNumber.setTextColor(amountAndUnitColor)
            textViewInvoiceAmountUnit.setTextColor(amountAndUnitColor)
        }
    }
}

// TODO: Refactor setting of spaces out of this extension function
@MainThread
internal fun LayoutMessageHolderBinding.setBubbleBackground(
    viewState: MessageHolderViewState,
    recyclerWidth: Px,
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

    val defaultMargins = root.context.resources
        .getDimensionPixelSize(common_R.dimen.default_layout_margin)

    if (viewState.background is BubbleBackground.Gone && viewState.background.setSpacingEqual) {

        spaceMessageHolderLeft.updateLayoutParams { width = defaultMargins }
        spaceMessageHolderRight.updateLayoutParams { width = defaultMargins }

    } else {
        val defaultReceivedLeftMargin = root.context.resources
            .getDimensionPixelSize(R.dimen.message_holder_space_width_left)

        val defaultSentRightMargin = root.context.resources
            .getDimensionPixelSize(R.dimen.message_holder_space_width_right)

        val holderWidth = recyclerWidth.value - (defaultMargins * 2)
        val bubbleFixedWidth = (holderWidth - defaultReceivedLeftMargin - defaultSentRightMargin - (holderWidth * BubbleBackground.SPACE_WIDTH_MULTIPLE)).toInt()

        val messageReactionsWidth = viewState.bubbleReactionBoosts?.let {
            root.context.resources.getDimensionPixelSize(R.dimen.message_type_boost_width)
        } ?: 0

        var bubbleWidth: Int = when {
            viewState.message?.shouldAdaptBubbleWidth == true -> {

                val textWidth = viewState.bubbleMessage?.let { nnBubbleMessage ->
                    (includeMessageHolderBubble.textViewMessageText.paint.measureText(
                        nnBubbleMessage.text ?: getString(R.string.decryption_error)
                    ) + (defaultMargins * 2)).toInt()
                } ?: 0

                val amountWidth = viewState.bubbleDirectPayment?.let { nnBubbleDirectPayment ->
                    val paymentMargin = root.context.resources.getDimensionPixelSize(
                        if (nnBubbleDirectPayment.isTribe) {
                            R.dimen.tribe_payment_row_margin
                        } else {
                            R.dimen.payment_row_margin
                        }
                    )

                    (includeMessageHolderBubble.includeMessageTypeDirectPayment.textViewSatsAmountReceived.paint.measureText(
                        nnBubbleDirectPayment.amount.asFormattedString()
                    ) + paymentMargin).toInt()
                } ?: 0

                val imageWidth = viewState.bubbleImageAttachment?.let {
                    (bubbleFixedWidth * 0.8F).toInt()
                } ?: 0

                textWidth
                    .coerceAtLeast(amountWidth)
                    .coerceAtLeast(imageWidth)
            }
            viewState.message?.isPodcastBoost == true -> {
                root.context.resources.getDimensionPixelSize(R.dimen.message_type_podcast_boost_width)
            }
            viewState.message?.isExpiredInvoice == true -> {
                root.context.resources.getDimensionPixelSize(R.dimen.message_type_expired_invoice_width)
            }
            viewState.message?.isSphinxCallLink == true -> {
                (bubbleFixedWidth * 0.8F).toInt()
            }
            else -> {
                bubbleFixedWidth
            }
        }

        bubbleWidth = bubbleWidth
            .coerceAtLeast(messageReactionsWidth)
            .coerceAtMost(bubbleFixedWidth)

        @Exhaustive
        when (viewState) {
            is MessageHolderViewState.Received -> {
                spaceMessageHolderLeft.updateLayoutParams {
                    width = defaultReceivedLeftMargin
                }
                spaceMessageHolderRight.updateLayoutParams {
                    width = (holderWidth - defaultReceivedLeftMargin - bubbleWidth).toInt()
                }
            }
            is MessageHolderViewState.Sent -> {
                spaceMessageHolderLeft.updateLayoutParams {
                    width = (holderWidth - defaultSentRightMargin - bubbleWidth).toInt()
                }
                spaceMessageHolderRight.updateLayoutParams {
                    width = defaultSentRightMargin
                }
            }
            is MessageHolderViewState.Separator -> { }

            is MessageHolderViewState.ThreadHeader -> { }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setMessagesSeparator(
    messagesSeparator: LayoutState.Separator?,
) {
    includeMessageTypeSeparatorHolder.apply {
        if (messagesSeparator == null) {
            root.gone
        } else {
            root.visible

            if (messagesSeparator.messageHolderType == MessageHolderType.DateSeparator) {
                includeMessageTypeUnseenSeparator.root.gone

                includeMessageTypeDateSeparator.apply {
                    root.visible

                    textViewDateSeparator.text = messagesSeparator.date?.let { date ->
                        when {
                            DateTime.getToday00().before(date) -> {
                                getString(R.string.today)
                            }
                            else -> {
                                DateTime.getFormatMMMEEEdd().format(date.value)
                            }
                        }
                    } ?: ""
                }
            } else if (messagesSeparator.messageHolderType == MessageHolderType.UnseenSeparator) {
                includeMessageTypeUnseenSeparator.root.visible
                includeMessageTypeDateSeparator.root.gone
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setSearchHighlightedStatus(
    searchStatus: LayoutState.SearchHighlightedStatus?
) {
    root.setBackgroundColor(
        if (searchStatus != null) {
            root.context.getColor(R.color.lightDivider)
        } else {
            root.context.getColor(android.R.color.transparent)
        }
    )
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setStatusHeader(
    statusHeader: LayoutState.MessageStatusHeader?,
    holderJobs: ArrayList<Job>,
    dispatchers: CoroutineDispatchers,
    lifecycleScope: CoroutineScope,
    userColorsHelper: UserColorsHelper,
) {
    includeMessageStatusHeader.apply {
        if (statusHeader == null) {
            root.gone

            includeMessageHolderChatImageInitialHolder.root.gone
        } else {
            root.visible

            includeMessageHolderChatImageInitialHolder.root.visible

            textViewMessageStatusReceivedSenderName.apply {
                statusHeader.senderName?.let { name ->
                    if (name.isEmpty()) {
                        gone
                    } else {
                        visible
                        text = name
                        lifecycleScope.launch(dispatchers.mainImmediate) {
                            textViewMessageStatusReceivedSenderName.setTextColor(
                                Color.parseColor(
                                    userColorsHelper.getHexCodeForKey(
                                        statusHeader.colorKey,
                                        root.context.getRandomHexCode()
                                    )
                                )
                            )
                        }.let { job ->
                            holderJobs.add(job)
                        }
                    }
                } ?: gone
            }

            layoutConstraintMessageStatusSentContainer.goneIfFalse(statusHeader.showSent)
            layoutConstraintMessageStatusReceivedContainer.goneIfFalse(statusHeader.showReceived)

            if (statusHeader.showSent) {
                textViewMessageStatusSentTimestamp.text = statusHeader.timestamp
                textViewMessageStatusSentLockIcon.goneIfFalse(statusHeader.showLockIcon)
                progressBarMessageStatusSending.goneIfFalse(statusHeader.showSendingIcon)
                textViewMessageStatusSentBoltIcon.goneIfFalse(statusHeader.showBoltIcon)
                layoutConstraintMessageStatusSentFailedContainer.goneIfFalse(statusHeader.showFailedContainer)

                if (statusHeader.errorMessage?.isNotEmpty() == true) {
                    textViewMessageStatusSentFailedText.text = statusHeader.errorMessage
                }

            } else {
                textViewMessageStatusReceivedTimestamp.text = statusHeader.timestamp
                textViewMessageStatusReceivedLockIcon.goneIfFalse(statusHeader.showLockIcon)
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setInvoiceExpirationHeader(
    invoiceExpirationHeader: LayoutState.InvoiceExpirationHeader?
) {
    includeInvoiceExpirationHeader.apply {
        if (invoiceExpirationHeader == null) {
            root.gone
        } else {
            root.visible

            layoutConstraintInvoiceExpirationReceivedContainer.goneIfFalse(invoiceExpirationHeader.showExpirationReceivedHeader)
            layoutConstraintInvoiceExpirationSentContainer.goneIfFalse(invoiceExpirationHeader.showExpirationSentHeader)

            val expirationText = when {
                invoiceExpirationHeader.showExpiredLabel -> {
                    getString(R.string.request_expired)
                }
                invoiceExpirationHeader.showExpiresAtLabel -> {
                    root.context.getString(
                        R.string.request_expiration,
                        invoiceExpirationHeader.expirationTimestamp ?: "-"
                    )
                }
                else -> {
                    ""
                }
            }

            textViewInvoiceExpirationReceivedText.text = expirationText
            textViewInvoiceExpirationSentText.text = expirationText
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setDeletedOrFlaggedMessageLayout(
    deletedOrFlaggedMessage: LayoutState.DeletedOrFlaggedMessage?
) {
    includeDeletedMessage.apply {
        if (deletedOrFlaggedMessage == null) {
            root.gone
        } else {
            root.visible

            val gravity = if (deletedOrFlaggedMessage.gravityStart) {
                Gravity.START
            } else {
                Gravity.END
            }

            textViewDeletedMessageTimestamp.text = deletedOrFlaggedMessage.timestamp
            textViewDeletedMessageTimestamp.gravity = gravity
            textViewDeleteMessageLabel.gravity = gravity

            textViewDeleteMessageLabel.text = root.context.getString(
                if (deletedOrFlaggedMessage.deleted) {
                    R.string.deleted_message_label_text
                } else {
                    R.string.flagged_message_label_text
                }
            )
        }
    }
}

@MainThread
internal fun LayoutMessageHolderBinding.setInvoiceDottedLinesLayout(
    viewState: MessageHolderViewState
) {
    includeMessageInvoiceDottedLinesHolder.apply {
        val invoice = viewState.bubbleInvoice

        if (invoice == null) {
            viewInvoiceBottomLeftLine.gone
            viewInvoiceBottomRightLine.gone
        } else {
            viewInvoiceBottomLeftLine.goneIfFalse(invoice.showReceived && invoice.showPaidInvoiceBottomLine)
            viewInvoiceBottomRightLine.goneIfFalse(invoice.showSent && invoice.showPaidInvoiceBottomLine)
        }
    }

    includeMessageInvoiceDottedLinesHolder.apply {
        val invoicePayment = viewState.invoicePayment

        if (invoicePayment == null) {
            layoutConstraintInvoicePaymentLeftLine.gone
            layoutConstraintInvoicePaymentRightLine.gone
        } else {
            layoutConstraintInvoicePaymentLeftLine.goneIfFalse(invoicePayment.showReceived)
            layoutConstraintInvoicePaymentRightLine.goneIfFalse(invoicePayment.showSent)
        }
    }

    includeMessageInvoiceDottedLinesHolder.apply {
        viewInvoiceLeftLine.goneIfFalse(viewState.invoiceLinesHolderViewState.left)
        viewInvoiceRightLine.goneIfFalse(viewState.invoiceLinesHolderViewState.right)
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setInvoicePaymentLayout(
    invoicePayment: LayoutState.InvoicePayment?
) {
    includeMessageTypeInvoicePayment.apply {
        if (invoicePayment == null) {
            root.gone
        } else {
            root.visible

            val gravity = if (invoicePayment.showReceived) {
                Gravity.START
            } else {
                Gravity.END
            }

            textViewInvoicePaymentDate.text = root.context.getString(R.string.invoice_paid_on, invoicePayment.paymentDateString)
            textViewInvoicePaymentDate.gravity = gravity
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleThreadLayout(
    thread: LayoutState.Bubble.ContainerThird.Thread?,
    holderJobs: ArrayList<Job>,
    dispatchers: CoroutineDispatchers,
    lifecycleScope: CoroutineScope,
    userColorsHelper: UserColorsHelper,
    audioPlayerController: AudioPlayerController,
    loadImage: (ImageView, String) -> Unit,
    lastReplyImage: (ImageView, ConstraintLayout, String, MessageMedia?) -> Unit
) {
    includeMessageHolderBubble.includeLayoutMessageThread.apply {
        if (thread == null) {
            root.gone
        } else {
            root.visible

            val users = thread.users

            setReplyRow(
                users.elementAtOrNull(0),
                layoutConstraintReplyOne,
                layoutConstraintReplyOneBackground,
                layoutLayoutChatImageSmallInitialHolderOne,
                holderJobs,
                dispatchers,
                lifecycleScope,
                userColorsHelper,
                null,
                thread.isSentMessage,
                false,
                loadImage
            )

            setReplyRow(
                users.elementAtOrNull(1),
                layoutConstraintReplyTwo,
                layoutConstraintReplyTwoBackground,
                layoutLayoutChatImageSmallInitialHolderTwo,
                holderJobs,
                dispatchers,
                lifecycleScope,
                userColorsHelper,
                null,
                thread.isSentMessage,
                false,
                loadImage
            )

            setReplyRow(
                thread.lastReplyUser,
                layoutConstraintLastReply,
                layoutConstraintLastReplyBackground,
                layoutLayoutChatImageSmallInitialHolderFour,
                holderJobs,
                dispatchers,
                lifecycleScope,
                userColorsHelper,
                null,
                thread.isSentMessage,
                true,
                loadImage
            )

            if (thread.replyCount > 3) {
                layoutConstraintReplyThree.visible

                layoutConstraintReplyThreeBackground.setBackgroundResource(
                    if (thread.isSentMessage) {
                        R.drawable.background_thread_row_reply_number_holder_sent
                    } else {
                        R.drawable.background_thread_row_reply_number_holder_received
                    }
                )
                textViewRepliesCount.text = thread.replyCount.minus(3).toString()
            }

            layoutConstraintLastReply.apply {
                textViewLastReplyMessageText.text = thread.lastReplyMessage
                textViewLastReplyDate.text = thread.lastReplyDate
                textViewLastReplyUserName.text = thread.lastReplyUser.alias?.value ?: "unknown"

                textViewLastReplyMessageText.goneIfFalse(thread.lastReplyMessage?.isNotEmpty() == true)
            }

            includeMessageTypeAudioAttachment.seekBarAttachmentAudio.thumb = null

            when (thread.mediaAttachment) {
                is LayoutState.Bubble.ContainerSecond.ImageAttachment -> {
                    constraintMediaThreadContainer.visible

                    includeMessageTypeImageAttachment.root.visible
                    includeMessageTypeVideoAttachment.root.gone
                    includeMessageTypeAudioAttachment.root.gone
                    includeMessageTypeFileAttachment.root.gone

                    setBubbleImageAttachment(thread.mediaAttachment, true, lastReplyImage)
                }
                is LayoutState.Bubble.ContainerSecond.VideoAttachment -> {
                    constraintMediaThreadContainer.visible

                    includeMessageTypeImageAttachment.root.gone
                    includeMessageTypeVideoAttachment.root.visible
                    includeMessageTypeAudioAttachment.root.gone
                    includeMessageTypeFileAttachment.root.gone

                    setBubbleVideoAttachment(thread.mediaAttachment, true)
                }
                is LayoutState.Bubble.ContainerSecond.AudioAttachment -> {
                    constraintMediaThreadContainer.visible

                    includeMessageTypeImageAttachment.root.gone
                    includeMessageTypeVideoAttachment.root.gone
                    includeMessageTypeAudioAttachment.root.visible
                    includeMessageTypeFileAttachment.root.gone

                    setBubbleAudioAttachment(
                        thread.mediaAttachment,
                        audioPlayerController,
                        dispatchers,
                        holderJobs,
                        lifecycleScope,
                        true)
                }
                is LayoutState.Bubble.ContainerSecond.FileAttachment -> {
                    constraintMediaThreadContainer.visible

                    includeMessageTypeImageAttachment.root.gone
                    includeMessageTypeVideoAttachment.root.gone
                    includeMessageTypeAudioAttachment.root.gone
                    includeMessageTypeFileAttachment.root.visible

                    setBubbleFileAttachment(
                        thread.mediaAttachment,
                        true
                    )
                }
                else -> {
                    constraintMediaThreadContainer.gone
                }
            }
        }
    }

}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleMessageLayout(
    message: LayoutState.Bubble.ContainerThird.Message?,
    onSphinxInteractionListener: SphinxUrlSpan.OnInteractionListener?
) {
    includeMessageHolderBubble.textViewMessageText.apply {
        if (message == null) {
            gone
        } else {
            includeMessageHolderBubble.textViewPaidMessageText.gone

            maxLines = if (message.isThread) 2 else Integer.MAX_VALUE
            visible
            text = message.text ?: getString(R.string.decryption_error)

            val textColor = ContextCompat.getColor(
                root.context,
                if (message.decryptionError) R.color.primaryRed else R.color.textMessages
            )
            setTextColor(textColor)

            if (onSphinxInteractionListener != null) {
                SphinxLinkify.addLinks(this, SphinxLinkify.ALL, includeMessageHolderBubble.root.context, onSphinxInteractionListener)
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubblePaidMessageLayout(
    dispatchers: CoroutineDispatchers,
    holderJobs: ArrayList<Job>,
    lifecycleScope: CoroutineScope,
    viewState: MessageHolderViewState,
    onSphinxInteractionListener: SphinxUrlSpan.OnInteractionListener?
) {
    includeMessageHolderBubble.textViewPaidMessageText.apply {
        val paidMessageViewStats = viewState.bubblePaidMessage

        if (paidMessageViewStats == null) {
            gone
        } else {
            includeMessageHolderBubble.textViewMessageText.gone

            visible

            text = if (paidMessageViewStats.showSent) {
                getString(R.string.paid_message_loading)
            } else {
                when (paidMessageViewStats.purchaseStatus) {
                    is PurchaseStatus.Pending -> {
                        getString(R.string.paid_message_pay_to_unlock)
                    }
                    is PurchaseStatus.Processing -> {
                        getString(R.string.paid_message_loading)
                    }
                    is PurchaseStatus.Denied -> {
                        getString(R.string.paid_message_unable_to_load)
                    }
                    is PurchaseStatus.Accepted -> {
                        getString(R.string.paid_message_loading)
                    }
                    else -> {
                        getString(R.string.paid_message_loading)
                    }
                }
            }

            lifecycleScope.launch(dispatchers.mainImmediate) {
                setBubbleMessageLayout(
                    viewState.retrievePaidTextMessageContent(),
                    onSphinxInteractionListener
                )
            }.let { job ->
                holderJobs.add(job)
            }
        }
    }
}

@MainThread
internal fun LayoutMessageHolderBinding.setBubbleMessageLinkPreviewLayout(
    dispatchers: CoroutineDispatchers,
    holderJobs: ArrayList<Job>,
    imageLoader: ImageLoader<ImageView>,
    lifecycleScope: CoroutineScope,
    viewState: MessageHolderViewState,
    onRowLayoutListener: MessageListAdapter.OnRowLayoutListener?,
) {
    includeMessageHolderBubble.apply {

    if (viewState.message?.thread?.isNotEmpty() == true) {
        includeMessageLinkPreviewContact.root.gone
        includeMessageLinkPreviewTribe.root.gone
        includeMessageLinkPreviewUrl.root.gone

        return
    }
        val previewLink = viewState.messageLinkPreview

        val placeHolderAndTextColor = ContextCompat.getColor(
            root.context,
            if (viewState.isReceived) R.color.secondaryText else R.color.secondaryTextSent
        )

        @Exhaustive
        when (previewLink) {
            null -> {
                includeMessageLinkPreviewContact.root.gone
                includeMessageLinkPreviewTribe.root.gone
                includeMessageLinkPreviewUrl.root.gone
            }
            is NodeDescriptor -> {

                includeMessageLinkPreviewTribe.root.gone
                includeMessageLinkPreviewUrl.root.gone

                includeMessageLinkPreviewContact.apply {
                    layoutConstraintContactLinkPreview.gone
                    layoutConstraintLinkPreviewContactDashedBorder.gone

                    textViewMessageLinkPreviewContactPubkey.text = previewLink.nodeDescriptor.value
                    textViewMessageLinkPreviewContactPubkey.setTextColor(placeHolderAndTextColor)

                    imageViewMessageLinkPreviewQrInviteIcon.setColorFilter(placeHolderAndTextColor, android.graphics.PorterDuff.Mode.SRC_IN)

                    imageViewMessageLinkPreviewContactAvatar.setColorFilter(placeHolderAndTextColor, android.graphics.PorterDuff.Mode.SRC_IN)
                    imageViewMessageLinkPreviewContactAvatar.setImageDrawable(
                        AppCompatResources.getDrawable(root.context, R.drawable.ic_add_contact)
                    )

                    viewLinkPreviewTribeDashedBorder.background = AppCompatResources.getDrawable(
                        root.context,
                        if (viewState.isReceived) R.drawable.background_received_rounded_corner_dashed_border_button else R.drawable.background_sent_rounded_corner_dashed_border_button
                    )

                    lifecycleScope.launch(dispatchers.mainImmediate) {
                        progressBarLinkPreview.visible

                        val state =
                            viewState.retrieveLinkPreview() as? LayoutState.Bubble.ContainerThird.LinkPreview.ContactPreview

                        if (state != null) {
                            textViewMessageLinkPreviewNewContactLabel.text = state.alias?.value ?: getString(R.string.new_contact)
                            state.photoUrl?.let { nnPhotoUrl ->

                                imageViewMessageLinkPreviewContactAvatar.clearColorFilter()

                                launch {
                                    imageLoader.load(
                                        imageViewMessageLinkPreviewContactAvatar,
                                        nnPhotoUrl.value,
                                        ImageLoaderOptions.Builder()
                                            .placeholderResId(R.drawable.ic_add_contact)
                                            .transformation(Transformation.CircleCrop)
                                            .build(),
                                    )
                                }.let { job ->
                                    holderJobs.add(job)
                                }
                            }
                            layoutConstraintLinkPreviewContactDashedBorder.goneIfFalse(state.showBanner)
                            textViewMessageLinkPreviewAddContactBanner.goneIfFalse(state.showBanner)
                        }

                        progressBarLinkPreview.gone
                        layoutConstraintContactLinkPreview.visible

                        onRowLayoutListener?.onRowHeightChanged()

                    }.let { job ->
                        holderJobs.add(job)
                    }

                    root.visible
                }
            }
            is TribeLink -> {
                includeMessageLinkPreviewContact.root.gone
                includeMessageLinkPreviewUrl.root.gone

                includeMessageLinkPreviewTribe.apply {

                    // reset view
                    layoutConstraintTribeLinkPreview.gone
                    layoutConstraintLinkPreviewTribeDashedBorder.gone
                    textViewMessageLinkPreviewTribeDescription.gone
                    textViewMessageLinkPreviewTribeNameLabel.gone
                    textViewMessageLinkPreviewTribeSeeBanner.gone

                    imageViewMessageLinkPreviewTribe.setColorFilter(placeHolderAndTextColor, android.graphics.PorterDuff.Mode.SRC_IN)

                    imageViewMessageLinkPreviewTribe.setImageDrawable(
                        AppCompatResources.getDrawable(root.context, R.drawable.ic_tribe)
                    )

                    viewLinkPreviewTribeDashedBorder.background = AppCompatResources.getDrawable(
                        root.context,
                        if (viewState.isReceived) R.drawable.background_received_rounded_corner_dashed_border_button else R.drawable.background_sent_rounded_corner_dashed_border_button
                    )

                    lifecycleScope.launch(dispatchers.mainImmediate) {
                        progressBarLinkPreview.visible

                        val state =
                            viewState.retrieveLinkPreview() as? LayoutState.Bubble.ContainerThird.LinkPreview.TribeLinkPreview

                        if (state != null) {
                            textViewMessageLinkPreviewTribeDescription.apply desc@ {
                                this@desc.text = state.description?.value
                                this@desc.goneIfTrue(state.description == null)
                            }
                            textViewMessageLinkPreviewTribeDescription.setTextColor(placeHolderAndTextColor)

                            textViewMessageLinkPreviewTribeNameLabel.apply name@ {
                                this@name.text = state.name.value
                                this@name.visible
                            }

                            state.imageUrl?.let { url ->
                                imageViewMessageLinkPreviewTribe.clearColorFilter()

                                launch {
                                    imageLoader.load(
                                        imageViewMessageLinkPreviewTribe,
                                        url.value,
                                        ImageLoaderOptions.Builder()
                                            .placeholderResId(R.drawable.ic_tribe)
                                            .transformation(Transformation.RoundedCorners(Px(5f),Px(5f),Px(5f),Px(5f)))
                                            .build(),
                                    )
                                }.let { job ->
                                    holderJobs.add(job)
                                }

                                onRowLayoutListener?.onRowHeightChanged()
                            }

                            layoutConstraintLinkPreviewTribeDashedBorder.goneIfFalse(state.showBanner)
                            textViewMessageLinkPreviewTribeSeeBanner.goneIfFalse(state.showBanner)
                        }

                        progressBarLinkPreview.gone
                        layoutConstraintTribeLinkPreview.visible

                    }.let { job ->
                        holderJobs.add(job)
                    }

                    root.visible
                }
            }
            is FeedItemPreview -> {
                includeMessageLinkPreviewContact.root.gone
                includeMessageLinkPreviewTribe.root.gone
                includeMessageLinkPreviewUrl.root.gone
            }
            is UnspecifiedUrl -> {
                includeMessageLinkPreviewContact.root.gone
                includeMessageLinkPreviewTribe.root.gone

                includeMessageLinkPreviewUrl.apply {

                    // reset view
                    layoutConstraintUrlLinkPreview.gone
                    textViewMessageLinkPreviewUrlDomain.gone
                    textViewMessageLinkPreviewUrlDescription.gone
                    textViewMessageLinkPreviewUrlTitle.gone
                    imageViewMessageLinkPreviewUrlFavicon.gone
                    imageViewMessageLinkPreviewUrlMainImage.gone


                    lifecycleScope.launch(dispatchers.mainImmediate) {
                        progressBarLinkPreview.visible

                        val state =
                            viewState.retrieveLinkPreview() as? LayoutState.Bubble.ContainerThird.LinkPreview.HttpUrlPreview

                        if (state != null) {
                            textViewMessageLinkPreviewUrlDomain.apply domain@ {
                                this@domain.text = state.domainHost.value
                                this@domain.visible
                            }
                            textViewMessageLinkPreviewUrlDescription.apply desc@ {
                                this@desc.text = state.description?.value
                                this@desc.goneIfTrue(state.description == null)
                            }
                            textViewMessageLinkPreviewUrlTitle.apply title@ {
                                this@title.text = state.title?.value
                                this@title.goneIfTrue( state.title == null)
                            }
                            imageViewMessageLinkPreviewUrlFavicon.apply favIcon@ {
                                state.favIconUrl?.let { url ->
                                    launch {
                                        imageLoader.load(
                                            imageView = this@favIcon,
                                            url = url.value,
                                        )
                                    }.let { job ->
                                        holderJobs.add(job)
                                    }
                                    this@favIcon.visible
                                }
                            }
                            imageViewMessageLinkPreviewUrlMainImage.apply main@ {
                                state.imageUrl?.let { url ->
                                    launch {
                                        imageLoader.load(
                                            imageView = this@main,
                                            url = url.value,
                                        )
                                    }.let { job ->
                                        holderJobs.add(job)
                                    }
                                    this@main.visible
                                }
                            }

                            progressBarLinkPreview.gone
                            layoutConstraintUrlLinkPreview.visible

                            onRowLayoutListener?.onRowHeightChanged()
                        }
                    }.let { job ->
                        holderJobs.add(job)
                    }

                    root.visible
                }
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleCallInvite(
    callInvite: LayoutState.Bubble.ContainerSecond.CallInvite?
) {
    includeMessageHolderBubble.includeMessageTypeCallInvite.apply {
        if (callInvite == null) {
            root.gone
        } else {
            root.visible
            layoutConstraintCallInviteJoinByVideo.goneIfFalse(callInvite.videoButtonVisible)
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleBotResponse(
    botResponse: LayoutState.Bubble.ContainerSecond.BotResponse?,
    onRowLayoutListener: MessageListAdapter.OnRowLayoutListener?,
) {
    includeMessageHolderBubble.includeMessageTypeBotResponse.apply {
        if (botResponse == null) {
            root.gone
        } else {
            root.visible

            val textColorString = getColorHexCode(R.color.text)
            val backgroundColorString = getColorHexCode(R.color.receivedMsgBG)

            val htmlPrefix = "<head><meta name=\"viewport\" content=\"width=device-width, height=device-height, shrink-to-fit=YES\"></head><body style=\"font-family: 'Roboto', sans-serif; color: $textColorString; margin:0px !important; padding:0px!important; background: $backgroundColorString;\"><div id=\"bot-response-container\" style=\"background: $backgroundColorString;\">"
            val htmlSuffix = "</div></body>"
            val contentHtml = htmlPrefix + botResponse.html + htmlSuffix

            webViewMessageTypeBotResponse.loadDataWithBaseURL(
                null,
                contentHtml,
                "text/html",
                "utf-8",
                null
            )

            webViewMessageTypeBotResponse.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    onRowLayoutListener?.onRowHeightChanged()
                }
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubblePaidMessageReceivedDetailsLayout(
    paidDetails: LayoutState.Bubble.ContainerFourth.PaidMessageReceivedDetails?,
    bubbleBackground: BubbleBackground
) {
    includeMessageHolderBubble.includePaidMessageReceivedDetailsHolder.apply {
        if (paidDetails == null) {
            root.gone
        } else {
            root.visible

            @ColorRes
            val backgroundTintResId = if (paidDetails.purchaseStatus is PurchaseStatus.Denied) {
                R.color.primaryRed
            } else {
                R.color.primaryGreen
            }

            @DrawableRes
            val backgroundDrawableResId: Int? = when (bubbleBackground) {
                BubbleBackground.First.Grouped -> {
                    R.drawable.background_paid_message_details_bubble_footer_received_first
                }
                BubbleBackground.First.Isolated,
                BubbleBackground.Last -> {
                    R.drawable.background_paid_message_details_bubble_footer_received_last
                }
                BubbleBackground.Middle -> {
                    R.drawable.background_paid_message_details_bubble_footer_received_middle
                }
                else -> {
                    null
                }
            }

            val statusText: String = when (paidDetails.purchaseStatus) {
                PurchaseStatus.Processing -> {
                    getString(R.string.purchase_status_label_paid_message_details_processing)
                }
                PurchaseStatus.Accepted -> {
                    getString(R.string.purchase_status_label_paid_message_details_accepted)
                }
                PurchaseStatus.Denied -> {
                    getString(R.string.purchase_status_label_paid_message_details_denied)
                }
                else -> {
                    getString(R.string.purchase_status_label_paid_message_details_default)
                }
            }

            val statusIcon: String = when (paidDetails.purchaseStatus) {
                PurchaseStatus.Accepted -> {
                    getString(R.string.material_icon_name_payment_accepted)
                }
                PurchaseStatus.Denied -> {
                    getString(R.string.material_icon_name_payment_denied)
                }
                else -> {
                    ""
                }
            }

            backgroundDrawableResId?.let { root.setBackgroundResource(it) }
            root.backgroundTintList = ContextCompat.getColorStateList(root.context, backgroundTintResId)

            textViewPaymentStatusIcon.text = statusIcon
            textViewPaymentStatusIcon.goneIfFalse(paidDetails.showStatusIcon)

            progressBarPaidMessage.goneIfFalse(paidDetails.showProcessingProgressBar)

            textViewPaidMessageStatusLabel.goneIfFalse(paidDetails.showStatusLabel)
            textViewPaidMessageStatusLabel.text = statusText

            buttonPayAttachment.goneIfFalse(paidDetails.showPayElements)
            textViewPayMessageLabel.goneIfFalse(paidDetails.showPayElements)
            imageViewPayMessageIcon.goneIfFalse(paidDetails.showPayElements)

            textViewPaidMessageAmountToPayLabel.text = paidDetails.amountText
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubblePaidMessageSentStatusLayout(
    paidSentStatus: LayoutState.Bubble.ContainerSecond.PaidMessageSentStatus?
) {
    includeMessageHolderBubble.includePaidMessageSentStatusDetails.apply {
        if (paidSentStatus == null) {
            root.gone
        } else {
            root.visible

            val statusTextResID = when (paidSentStatus.purchaseStatus) {
                PurchaseStatus.Pending -> {
                    R.string.purchase_status_label_paid_message_sent_status_pending
                }
                PurchaseStatus.Processing -> {
                    R.string.purchase_status_label_paid_message_sent_status_processing
                }
                PurchaseStatus.Accepted -> {
                    R.string.purchase_status_label_paid_message_sent_status_accepted
                }
                PurchaseStatus.Denied -> {
                    R.string.purchase_status_label_paid_message_sent_status_denied
                }
                null -> {
                    R.string.purchase_status_label_paid_message_sent_status_pending
                }
            }

            textViewPaidMessageSentStatusAmount.text = paidSentStatus.amountText
            textViewPaidMessageSentStatus.text = getString(statusTextResID)
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleImageAttachment(
    imageAttachment: LayoutState.Bubble.ContainerSecond.ImageAttachment?,
    isThread: Boolean? = null,
    loadImage: (ImageView, ConstraintLayout, String, MessageMedia?) -> Unit,
) {
    val currentLayout = if (isThread == true) {
        includeMessageHolderBubble.includeLayoutMessageThread.includeMessageTypeImageAttachment
    } else {
        includeMessageHolderBubble.includeMessageTypeImageAttachment
    }

    currentLayout.apply {
        if (imageAttachment == null) {
            root.gone
        } else {
            root.visible

            val image = imageViewAttachmentImage

            if (imageAttachment.showPaidOverlay) {
                layoutConstraintPaidImageOverlay.visible

                image.gone
            } else {
                layoutConstraintPaidImageOverlay.gone

                loadingImageProgressContainer.visible
                image.visible

                if (imageAttachment.isThread) {
                    val params = image.layoutParams as FrameLayout.LayoutParams
                    params.height = 460

                    image.layoutParams = params
                }

                loadImage(image, loadingImageProgressContainer, imageAttachment.url, imageAttachment.media)
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleAudioAttachment(
    audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment?,
    audioPlayerController: AudioPlayerController,
    dispatchers: CoroutineDispatchers,
    holderJobs: ArrayList<Job>,
    lifecycleScope: CoroutineScope,
    isThread: Boolean? = null
) {
    val currentLayout = if (isThread == true) {
        includeMessageHolderBubble.includeLayoutMessageThread.includeMessageTypeAudioAttachment
    } else {
        includeMessageHolderBubble.includeMessageTypeAudioAttachment
    }

    currentLayout.apply {
        @Exhaustive
        when (audioAttachment) {
            null -> {
                root.gone
            }
            is LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable -> {
                root.visible
                lifecycleScope.launch(dispatchers.io) {
                    audioPlayerController.getAudioState(audioAttachment)?.value?.let { state ->
                        lifecycleScope.launch(dispatchers.mainImmediate) {
                            setAudioAttachmentLayoutForState(state)
                        }
                    } ?: run {
                        lifecycleScope.launch(dispatchers.mainImmediate) {
                            setAudioAttachmentLayoutForState(
                                AudioMessageState(
                                    audioAttachment.messageId,
                                    null,
                                    null,
                                    null,
                                    AudioPlayState.Error,
                                    1L,
                                    0L,
                                )
                            )
                        }
                    }
                }.let { job ->
                    holderJobs.add(job)
                }
            }
            is LayoutState.Bubble.ContainerSecond.AudioAttachment.FileUnavailable -> {
                root.visible
                setAudioAttachmentLayoutForState(
                    AudioMessageState(
                        audioAttachment.messageId,
                        null,
                        null,
                        null,
                        AudioPlayState.Loading,
                        1L,
                        0L
                    )
                )
            }
        }

    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageTypeAttachmentAudioBinding.setAudioAttachmentLayoutForState(
    state: AudioMessageState
) {

    seekBarAttachmentAudio.progress = state.progress.toInt()
    textViewAttachmentAudioRemainingDuration.text = state.remainingSeconds.getHHMMString()


    @Exhaustive
    when (state.playState) {
        AudioPlayState.Error -> {
            textViewAttachmentAudioFailure.visible
            textViewAttachmentPlayPauseButton.gone
            progressBarAttachmentAudioFileLoading.gone
        }
        AudioPlayState.Loading -> {
            textViewAttachmentAudioFailure.gone
            textViewAttachmentPlayPauseButton.gone
            progressBarAttachmentAudioFileLoading.visible
        }
        AudioPlayState.Paused -> {
            progressBarAttachmentAudioFileLoading.gone
            textViewAttachmentAudioFailure.gone

            textViewAttachmentPlayPauseButton.text = getString(R.string.material_icon_name_play_button)
            textViewAttachmentPlayPauseButton.visible
        }
        AudioPlayState.Playing -> {
            progressBarAttachmentAudioFileLoading.gone
            textViewAttachmentAudioFailure.gone

            textViewAttachmentPlayPauseButton.text = getString(R.string.material_icon_name_pause_button)
            textViewAttachmentPlayPauseButton.visible

        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubblePodcastClip(
    podcastClipViewState: LayoutState.Bubble.ContainerSecond.PodcastClip?,
    audioPlayerController: AudioPlayerController,
    dispatchers: CoroutineDispatchers,
    holderJobs: ArrayList<Job>,
    lifecycleScope: CoroutineScope,
) {
    includeMessageHolderBubble.includeMessageTypePodcastClip.apply {
        if (podcastClipViewState == null) {
            root.gone
        } else {
            root.visible
            textViewPodcastEpisodeTitle.text = podcastClipViewState.podcastClip.title

            lifecycleScope.launch(dispatchers.io) {
                audioPlayerController.getAudioState(podcastClipViewState)?.value?.let { state ->
                    lifecycleScope.launch(dispatchers.mainImmediate) {
                        setPodcastClipLayoutForState(state)
                    }
                } ?: run {
                    lifecycleScope.launch(dispatchers.mainImmediate) {
                        setPodcastClipLayoutForState(
                            AudioMessageState(
                                podcastClipViewState.messageId,
                                podcastClipViewState.messageUUID,
                                null,
                                podcastClipViewState.podcastClip,
                                AudioPlayState.Error,
                                1L,
                                0L,
                            )
                        )
                    }
                }
            }.let { job ->
                holderJobs.add(job)
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageTypePodcastClipBinding.setPodcastClipLayoutForState(
    state: AudioMessageState
) {

    seekBarPodcastClip.progress = state.progress.toInt()

    textViewPodcastClipCurrentTime.text = state.currentMillis.getHHMMSSString()
    textViewPodcastClipDuration.text = state.durationMillis.getHHMMSSString()

    @Exhaustive
    when (state.playState) {
        AudioPlayState.Error -> {
            textViewPodcastClipFailure.visible
            layoutConstraintPlayPauseButton.gone
            progressBarPodcastClipLoading.gone
        }
        AudioPlayState.Loading -> {
            textViewPodcastClipFailure.gone
            layoutConstraintPlayPauseButton.gone
            progressBarPodcastClipLoading.visible
        }
        AudioPlayState.Paused -> {
            textViewPodcastClipFailure.gone
            progressBarPodcastClipLoading.gone

            textViewPodcastClipPlayPauseButton.text = getString(R.string.material_icon_name_play_button)
            layoutConstraintPlayPauseButton.visible
        }
        AudioPlayState.Playing -> {
            textViewPodcastClipFailure.gone
            progressBarPodcastClipLoading.gone

            textViewPodcastClipPlayPauseButton.text = getString(R.string.material_icon_name_pause_button)
            layoutConstraintPlayPauseButton.visible
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleVideoAttachment(
    videoAttachment: LayoutState.Bubble.ContainerSecond.VideoAttachment?,
    isThread: Boolean? = null
) {
    val currentLayout = if (isThread == true) {
        includeMessageHolderBubble.includeLayoutMessageThread.includeMessageTypeVideoAttachment
    } else {
        includeMessageHolderBubble.includeMessageTypeVideoAttachment
    }

    currentLayout.apply {
        imageViewAttachmentThumbnail.gone
        layoutConstraintVideoPlayButton.gone

        @Exhaustive
        when (videoAttachment) {
            null -> {
                root.gone
            }
            is LayoutState.Bubble.ContainerSecond.VideoAttachment.FileAvailable -> {
                root.visible

                val thumbnail = VideoThumbnailUtil.loadThumbnail(videoAttachment.file)

                if (thumbnail != null) {
                    imageViewAttachmentThumbnail.setImageBitmap(thumbnail)
                    layoutConstraintVideoPlayButton.visible
                }

                imageViewAttachmentThumbnail.visible
            }
            is LayoutState.Bubble.ContainerSecond.VideoAttachment.FileUnavailable -> {
                if  (videoAttachment.showPaidOverlay) {
                    layoutConstraintPaidVideoOverlay.visible

                    imageViewAttachmentThumbnail.gone
                } else {
                    layoutConstraintPaidVideoOverlay.gone

                    imageViewAttachmentThumbnail.visible
                }
                root.visible
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleFileAttachment(
    fileAttachment: LayoutState.Bubble.ContainerSecond.FileAttachment?,
    isThread: Boolean? = null
) {
    val currentLayout = if (isThread == true) {
        includeMessageHolderBubble.includeLayoutMessageThread.includeMessageTypeFileAttachment
    } else {
        includeMessageHolderBubble.includeMessageTypeFileAttachment
    }

    currentLayout.apply {

        @Exhaustive
        when (fileAttachment){
            null -> {
                root.gone
            }
            is LayoutState.Bubble.ContainerSecond.FileAttachment.FileAvailable -> {
                root.visible

                progressBarAttachmentFileDownload.gone
                buttonAttachmentFileDownload.visible

                textViewAttachmentFileIcon.text = if (fileAttachment.isPdf) {
                    getString(R.string.material_icon_name_file_pdf)
                } else {
                    getString(R.string.material_icon_name_file_attachment)
                }

                textViewAttachmentFileName.text = fileAttachment.fileName?.value ?: "File.txt"

                textViewAttachmentFileSize.text = if (fileAttachment.isPdf) {
                    if (fileAttachment.pageCount > 1) {
                        "${fileAttachment.pageCount} ${getString(R.string.pdf_pages)}"
                    } else {
                        "${fileAttachment.pageCount} ${getString(R.string.pdf_page)}"
                    }
                } else {
                    fileAttachment.fileSize.asFormattedString()
                }
            }
            is LayoutState.Bubble.ContainerSecond.FileAttachment.FileUnavailable -> {
                root.visible

                textViewAttachmentFileIcon.text = getString(R.string.material_icon_name_file_attachment)

                textViewAttachmentFileName.text = if (fileAttachment.pendingPayment) {
                    getString(R.string.paid_file_pay_to_unlock)
                } else {
                    getString(R.string.file_name_loading)
                }

                textViewAttachmentFileSize.text = "-"

                progressBarAttachmentFileDownload.goneIfFalse(
                    !fileAttachment.pendingPayment
                )
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubblePodcastBoost(
    podcastBoost: LayoutState.Bubble.ContainerSecond.PodcastBoost?
) {
    includeMessageHolderBubble.includeMessageTypePodcastBoost.apply {
        if (podcastBoost == null) {
            root.gone
        } else {
            root.visible

            textViewPodcastBoostAmount.text = podcastBoost.amount.asFormattedString()
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleReactionBoosts(
    boost: LayoutState.Bubble.ContainerFourth.Boost?,
    holderJobs: ArrayList<Job>,
    dispatchers: CoroutineDispatchers,
    lifecycleScope: CoroutineScope,
    userColorsHelper: UserColorsHelper,
    loadImage: (ImageView, String) -> Unit,
) {
    includeMessageHolderBubble.includeMessageTypeBoost.apply {
        if (boost == null) {
            root.gone
        } else {
            root.visible

            val activeIcon = boost.boostedByOwner || boost.showSent

            imageViewBoostMessageIcon.setImageDrawable(
                AppCompatResources.getDrawable(root.context,
                    if (activeIcon) {
                        R.drawable.ic_boost_green
                    } else {
                        R.drawable.ic_boost_grey
                    }
                )
            )

            includeBoostAmountTextGroup.apply {
                val textSizeInPixels = root.context.resources.getDimension(
                    if (boost.showSent) {
                        R.dimen.default_text_size_small_headline
                    } else {
                        R.dimen.default_text_size_sub_headline
                    }
                )
                textViewSatsAmount.textSize = Px(textSizeInPixels).toSp(root.context).value

                textViewSatsAmount.setTextFont(
                    if (boost.showSent) {
                        R.font.roboto_medium
                    } else {
                        R.font.roboto_regular
                    }
                )

                textViewSatsAmount.text = boost.amountText
                textViewSatsUnitLabel.text = boost.amountUnitLabel
            }

            includeBoostReactionsGroup.apply {

                setReactionBoostSender(
                    boost.senders.elementAtOrNull(0),
                    layoutConstraintBoostReactionImageHolder1,
                    includeBoostReactionImageHolder1,
                    holderJobs,
                    dispatchers,
                    lifecycleScope,
                    userColorsHelper,
                    loadImage,
                )

                setReactionBoostSender(
                    boost.senders.elementAtOrNull(1),
                    layoutConstraintBoostReactionImageHolder2,
                    includeBoostReactionImageHolder2,
                    holderJobs,
                    dispatchers,
                    lifecycleScope,
                    userColorsHelper,
                    loadImage,
                )

                setReactionBoostSender(
                    boost.senders.elementAtOrNull(2),
                    layoutConstraintBoostReactionImageHolder3,
                    includeBoostReactionImageHolder3,
                    holderJobs,
                    dispatchers,
                    lifecycleScope,
                    userColorsHelper,
                    loadImage,
                )

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
internal inline fun LayoutMessageHolderBinding.setReactionBoostSender(
    boostSenderHolder: BoostSenderHolder?,
    container: ConstraintLayout,
    imageHolderBinding: LayoutChatImageSmallInitialHolderBinding,
    holderJobs: ArrayList<Job>,
    dispatchers: CoroutineDispatchers,
    lifecycleScope: CoroutineScope,
    userColorsHelper: UserColorsHelper,
    loadImage: (ImageView, String) -> Unit,
) {
    container.let { imageHolderContainer ->
        if (boostSenderHolder == null) {
            imageHolderContainer.gone
        } else {
            imageHolderContainer.visible

            imageHolderBinding.apply {

                textViewInitialsName.visible
                textViewInitialsName.text = (boostSenderHolder.alias?.value ?: root.context.getString(R.string.unknown)).getInitials()
                imageViewChatPicture.gone

                lifecycleScope.launch(dispatchers.mainImmediate) {
                    textViewInitialsName.setBackgroundRandomColor(
                        R.drawable.chat_initials_circle,
                        Color.parseColor(
                            userColorsHelper.getHexCodeForKey(
                                boostSenderHolder.colorKey,
                                root.context.getRandomHexCode(),
                            )
                        ))
                }.let { job ->
                    holderJobs.add(job)
                }

                boostSenderHolder.photoUrl?.let { photoUrl ->
                    textViewInitialsName.gone
                    imageViewChatPicture.visible
                    loadImage(imageViewChatPicture, photoUrl.value)
                }
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setReplyRow(
    replyUserHolder: ReplyUserHolder?,
    container: ConstraintLayout,
    backgroundContainer: ConstraintLayout,
    imageHolderBinding: LayoutChatImageSmallInitialHolderBinding,
    holderJobs: ArrayList<Job>,
    dispatchers: CoroutineDispatchers,
    lifecycleScope: CoroutineScope,
    userColorsHelper: UserColorsHelper,
    repliesNumber: String? = null,
    isSentMessage: Boolean,
    isLastReply: Boolean = false,
    loadImage: (ImageView, String) -> Unit,
) {
    container.let { replyContainer ->
        if (replyUserHolder == null) {
            replyContainer.gone
        } else {
            replyContainer.visible

            imageHolderBinding.apply {

                val rowBackground = when {
                    isLastReply -> if (isSentMessage) {
                        R.drawable.background_thread_row_last_reply_holder_sent
                    } else {
                        R.drawable.background_thread_row_last_reply_holder_received
                    }
                    else -> if (isSentMessage) {
                        R.drawable.background_thread_row_reply_holder_sent
                    } else {
                        R.drawable.background_thread_row_reply_holder_received
                    }
                }

                backgroundContainer.setBackgroundResource(rowBackground)

                val text = (replyUserHolder?.alias?.value ?: root.context.getString(
                    R.string.unknown
                )).getInitials()

                textViewInitialsName.visible
                textViewInitialsName.text = text
                imageViewChatPicture.gone

                lifecycleScope.launch(dispatchers.mainImmediate) {
                    textViewInitialsName.setBackgroundRandomColor(
                        R.drawable.chat_initials_circle,
                        Color.parseColor(
                            userColorsHelper.getHexCodeForKey(
                                replyUserHolder!!.colorKey,
                                root.context.getRandomHexCode(),
                            )
                        )
                    )
                }.let { job ->
                    holderJobs.add(job)
                }

                replyUserHolder!!.photoUrl?.let { photoUrl ->
                    textViewInitialsName.gone
                    imageViewChatPicture.visible
                    loadImage(imageViewChatPicture, photoUrl.value)
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
                    setGroupActionJoinApprovedAdminLayout(groupActionDetails)
                } else {
                    setGroupActionAnnouncementLayout(groupActionDetails)
                }
            }
            MessageType.GroupAction.MemberReject -> {
                if (groupActionDetails.isAdminView) {
                    setGroupActionJoinRejectedAdminLayout(groupActionDetails)
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
    includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionJoinRequest.root.gone

    includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionAnnouncement.apply {
        root.visible

        val actionLabelText = when (groupActionDetails.actionType) {
            MessageType.GroupAction.Join -> {
                if (groupActionDetails.chatType == ChatType.Tribe) {
                    root.context.getString(R.string.tribe_join_announcement, groupActionDetails.subjectName)
                } else {
                    root.context.getString(R.string.group_join_announcement, groupActionDetails.subjectName)
                }
            }
            MessageType.GroupAction.Leave -> {
                if (groupActionDetails.chatType == ChatType.Tribe) {
                    root.context.getString(R.string.tribe_leave_announcement, groupActionDetails.subjectName)
                } else {
                    root.context.getString(R.string.group_leave_announcement, groupActionDetails.subjectName)
                }
            }
            MessageType.GroupAction.MemberApprove -> {
                if (groupActionDetails.chatType == ChatType.Tribe) {
                    root.context.getString(R.string.tribe_welcome_announcement_member_side)
                } else {
                    null
                }
            }
            else -> {
                null
            }
        }

        actionLabelText?.let {
            textViewGroupActionAnnouncementLabel.text = it
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
    includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionAnnouncement.root.gone

    includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionJoinRequest.apply {
        root.visible

        textViewGroupActionJoinRequestMessage.text = root.context.getString(R.string.tribe_request_admin_side, groupActionDetails.subjectName)

        textViewGroupActionJoinRequestAcceptAction.isEnabled = true
        textViewGroupActionJoinRequestAcceptAction.alpha = 1.0f

        textViewGroupActionJoinRequestRejectAction.isEnabled = true
        textViewGroupActionJoinRequestRejectAction.alpha = 1.0f
    }
}

/**
 * Presents a view for an admin to handle see rejected group membership requests
 */
@MainThread
@Suppress("NOTHING_TO_INLINE")
private inline fun LayoutMessageHolderBinding.setGroupActionJoinRejectedAdminLayout(
    groupActionDetails: LayoutState.GroupActionIndicator
) {
    includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionJoinRequest.apply {
        root.visible

        textViewGroupActionJoinRequestMessage.text = root.context.getString(R.string.tribe_request_rejected_admin_side, groupActionDetails.subjectName)

        textViewGroupActionJoinRequestAcceptAction.isEnabled = false
        textViewGroupActionJoinRequestAcceptAction.alpha = 0.2f

        textViewGroupActionJoinRequestRejectAction.isEnabled = false
        textViewGroupActionJoinRequestRejectAction.alpha = 1.0f
    }
}

/**
 * Presents a view for an admin to handle see group approved membership
 */
@MainThread
@Suppress("NOTHING_TO_INLINE")
private inline fun LayoutMessageHolderBinding.setGroupActionJoinApprovedAdminLayout(
    groupActionDetails: LayoutState.GroupActionIndicator
) {
    includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionJoinRequest.apply {
        root.visible

        textViewGroupActionJoinRequestMessage.text = root.context.getString(R.string.tribe_request_approved_admin_side, groupActionDetails.subjectName)

        textViewGroupActionJoinRequestRejectAction.isEnabled = false
        textViewGroupActionJoinRequestRejectAction.alpha = 0.2f

        textViewGroupActionJoinRequestAcceptAction.isEnabled = false
        textViewGroupActionJoinRequestAcceptAction.alpha = 1.0f
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

        textViewGroupActionMemberRemovalMessage.text = root.context.getString(
            when (groupActionDetails.actionType) {
                is MessageType.GroupAction.Kick -> {
                    R.string.tribe_kick_announcement_member_side
                }
                is MessageType.GroupAction.MemberReject -> {
                    R.string.tribe_request_rejected_member_side
                }
                else -> R.string.tribe_deleted_announcement
            }
        )
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleReplyMessage(
    replyMessage: LayoutState.Bubble.ContainerFirst.ReplyMessage?,
    holderJobs: ArrayList<Job>,
    dispatchers: CoroutineDispatchers,
    lifecycleScope: CoroutineScope,
    userColorsHelper: UserColorsHelper,
    loadImage: (ImageView, String, MessageMedia?) -> Unit
) {
    includeMessageHolderBubble.includeMessageReply.apply {
        if (replyMessage == null) {
            root.gone
        } else {
            root.visible

            imageViewReplyMediaImage.apply {
                if (replyMessage.url != null) {
                    visible

                    loadImage(this, replyMessage.url, replyMessage.media)
                } else {
                    gone
                }
            }
            textViewReplyTextOverlay.gone

            // Only used in the footer when replying to a message
            textViewReplyClose.gone

            viewReplyBarLeading.setBackgroundColor(root.context.getColor(R.color.lightPurple))

            layoutConstraintMessageReplyDividerBottom.setBackgroundColor(root.context.getColor(
                if (replyMessage.showReceived) {
                    R.color.replyDividerReceived
                } else {
                    R.color.replyDividerSent
                }
            ))

            textViewReplyMessageLabel.setTextColor(root.context.getColor(
                if (replyMessage.showReceived) {
                    R.color.washedOutReceivedText
                } else {
                    R.color.washedOutSentText
                }
            ))

            textViewReplySenderLabel.text = replyMessage.sender

            lifecycleScope.launch(dispatchers.mainImmediate) {
                viewReplyBarLeading.setBackgroundRandomColor(
                    null,
                    Color.parseColor(
                        userColorsHelper.getHexCodeForKey(
                            replyMessage.colorKey,
                            root.context.getRandomHexCode(),
                        )
                    )
                )
            }.let { job ->
                holderJobs.add(job)
            }

            if (replyMessage.isAudio) {
                textViewReplyTextOverlay.text = getString(R.string.material_icon_name_volume_up)
                textViewReplyTextOverlay.visible

                textViewReplyMessageLabel.text = getString(R.string.media_type_label_audio)
                textViewReplyMessageLabel.goneIfFalse(true)
            } else {
                textViewReplyMessageLabel.text = replyMessage.text
                textViewReplyMessageLabel.goneIfFalse(replyMessage.text.isNotEmpty())
            }
        }
    }
}
