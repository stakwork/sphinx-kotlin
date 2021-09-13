package chat.sphinx.chat_common.ui.viewstate.messageholder

import android.graphics.Color
import android.media.MediaPlayer
import android.view.Gravity
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.chat_common.databinding.LayoutMessageTypeAttachmentAudioBinding
import chat.sphinx.chat_common.model.NodeDescriptor
import chat.sphinx.chat_common.model.TribeLink
import chat.sphinx.chat_common.model.UnspecifiedUrl
import chat.sphinx.chat_common.ui.retrieveRemoteMediaInputStream
import chat.sphinx.chat_common.util.MessageMediaPlayer
import chat.sphinx.chat_common.util.SphinxLinkify
import chat.sphinx.chat_common.util.SphinxUrlSpan
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_meme_input_stream.MemeInputStreamHandler
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_client_crypto.CryptoHeader
import chat.sphinx.concept_network_client_crypto.CryptoScheme
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.resources.*
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_meme_server.headerKey
import chat.sphinx.wrapper_meme_server.headerValue
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.MessageMedia
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import chat.sphinx.resources.R as common_R

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal fun LayoutMessageHolderBinding.setView(
    lifecycleScope: CoroutineScope,
    holderJobs: ArrayList<Job>,
    disposables: ArrayList<Disposable>,
    dispatchers: CoroutineDispatchers,
    imageLoader: ImageLoader<ImageView>,
    imageLoaderDefaults: ImageLoaderOptions,
    memeServerTokenHandler: MemeServerTokenHandler,
    memeInputStreamHandler: MemeInputStreamHandler,
    mediaCacheHandler: MediaCacheHandler,
    messageMediaPlayer: MessageMediaPlayer,
    repositoryMedia: RepositoryMedia,
    recyclerViewWidth: Px,
    viewState: MessageHolderViewState,
    userColorsHelper: UserColorsHelper,
    onSphinxInteractionListener: SphinxUrlSpan.OnInteractionListener? = null,
) {
    for (job in holderJobs) {
        job.cancel()
    }
    holderJobs.clear()

    for (disposable in disposables) {
        disposable.dispose()
    }
    disposables.clear()

    apply {
        lifecycleScope.launch(dispatchers.mainImmediate) {
            val initialsColor = viewState.statusHeader?.colorKey?.let { key ->
                Color.parseColor(
                    userColorsHelper.getHexCodeForKey(key, root.context.getRandomHexCode())
                )
            }

            viewState.initialHolder.setInitialHolder(
                includeMessageHolderChatImageInitialHolder.textViewInitials,
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

        setStatusHeader(
            viewState.statusHeader,
            holderJobs,
            dispatchers,
            lifecycleScope,
            userColorsHelper,
        )
        setDeletedMessageLayout(viewState.deletedMessage)
        setBubbleBackground(viewState, recyclerViewWidth)
        setGroupActionIndicatorLayout(viewState.groupActionIndicator)

        if (viewState.background !is BubbleBackground.Gone) {
            setBubbleImageAttachment(viewState.bubbleImageAttachment) { imageView, url, media ->
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
            setBubbleAudioAttachment(viewState.bubbleAudioAttachment) { layoutMessageAudioAttachment, url, media ->
                lifecycleScope.launch(dispatchers.mainImmediate) {
                    layoutMessageAudioAttachment.apply {
                        val onPlayProgressInfoUpdateListener = object: MessageMediaPlayer.OnPlayProgressInfoUpdateListener {
                            override fun onPlayProgressUpdate(millisUntilFinished: Long) {
                                lifecycleScope.launch(dispatchers.mainImmediate) {
                                    textViewAttachmentAudioRemainingDuration.text = millisUntilFinished.toTimestamp()
                                    seekBarAttachmentAudio.progress = messageMediaPlayer.currentPosition
                                }
                            }

                            override fun onFinish() {
                                lifecycleScope.launch(dispatchers.mainImmediate) {
                                    textViewAttachmentAudioRemainingDuration.text = messageMediaPlayer.duration.toLong().toTimestamp()
                                    seekBarAttachmentAudio.progress = 0
                                }
                            }

                        }

                        val filePath: String? = media?.localFile?.absolutePath ?: media?.retrieveRemoteMediaInputStream(
                                url,
                                memeServerTokenHandler,
                                memeInputStreamHandler
                            )?.let { inputStream ->
                                // TODO: Determine extension from media.mediaType
                                val localAudioFile = mediaCacheHandler.createAudioFile(".m4a")
                                mediaCacheHandler.copyTo(inputStream, localAudioFile)
                                repositoryMedia.updateLocalFile(
                                    localAudioFile,
                                    viewState.message.id
                                )
                                localAudioFile.absolutePath
                            }

                        if (filePath == null) {
                            // Filed to load a file
                            progressBarAttachmentAudioFileLoading.gone
                            textViewAttachmentAudioFailure.visible
                        } else {
                            if (messageMediaPlayer.filePath == filePath) {
                                // The messageMediaPlayer is playing this specific file
                                seekBarAttachmentAudio.max = messageMediaPlayer.duration
                                seekBarAttachmentAudio.progress = messageMediaPlayer.currentPosition
                                textViewAttachmentAudioRemainingDuration.text = messageMediaPlayer.duration.toLong().toTimestamp()
                                progressBarAttachmentAudioFileLoading.gone
                                textViewAttachmentPlayPauseButton.visible

                                messageMediaPlayer.initPlayProgressInfoUpdateWithTimer(
                                    seekBarAttachmentAudio.progress,
                                    onPlayProgressInfoUpdateListener
                                )
                            } else {
                                // Load the audio file and forget it
                                MediaPlayer().apply {
                                    try {
                                        setDataSource(
                                            filePath
                                        )
                                        setOnPreparedListener {
                                            seekBarAttachmentAudio.max = duration
                                            textViewAttachmentAudioRemainingDuration.text = duration.toLong().toTimestamp()
                                            progressBarAttachmentAudioFileLoading.gone
                                            textViewAttachmentPlayPauseButton.visible

                                            // Finished loading the media...
                                            release()
                                        }
                                        setOnErrorListener { mp, what, extra ->
                                            progressBarAttachmentAudioFileLoading.gone
                                            textViewAttachmentAudioFailure.visible

                                            return@setOnErrorListener true
                                        }

                                        prepareAsync()

                                    } catch (e: IOException) {
                                        progressBarAttachmentAudioFileLoading.gone
                                        textViewAttachmentAudioFailure.visible
                                    }
                                }

                            }


                            textViewAttachmentPlayPauseButton.setOnClickListener {
                                if (messageMediaPlayer.filePath != filePath) {
                                    progressBarAttachmentAudioFileLoading.visible
                                    textViewAttachmentPlayPauseButton.gone
                                    textViewAttachmentAudioFailure.gone
                                    messageMediaPlayer.reset()

                                    messageMediaPlayer.apply {
                                        try {
                                            setOnPreparedListener {
                                                seekBarAttachmentAudio.max = duration
                                                textViewAttachmentAudioRemainingDuration.text = duration.toLong().toTimestamp()
                                                progressBarAttachmentAudioFileLoading.gone
                                                textViewAttachmentPlayPauseButton.visible

                                                start()
                                            }

                                            setOnErrorListener { mp, what, extra ->
                                                progressBarAttachmentAudioFileLoading.gone
                                                textViewAttachmentAudioFailure.visible

                                                return@setOnErrorListener true
                                            }
                                            load(filePath)


                                        } catch (e: IOException) {
                                            progressBarAttachmentAudioFileLoading.gone
                                            textViewAttachmentAudioFailure.visible
                                        }

                                    }
                                }


                                if (messageMediaPlayer.isPlaying) {
                                    messageMediaPlayer.pause()
                                    textViewAttachmentPlayPauseButton.text = getString(R.string.material_icon_name_play_button)
                                } else {
                                    textViewAttachmentPlayPauseButton.text = getString(R.string.material_icon_name_pause_button)
                                    messageMediaPlayer.start()
                                    messageMediaPlayer.initPlayProgressInfoUpdateWithTimer(
                                        seekBarAttachmentAudio.progress,
                                        onPlayProgressInfoUpdateListener
                                    )
                                }
                            }
                        }

                    }


                }.let { job ->
                    holderJobs.add(job)
                }



            }
            setUnsupportedMessageTypeLayout(viewState.unsupportedMessageType)
            setBubbleMessageLayout(viewState.bubbleMessage, onSphinxInteractionListener)
            setBubbleMessageLinkPreviewLayout(
                dispatchers,
                holderJobs,
                imageLoader,
                lifecycleScope,
                viewState
            )
            setBubbleCallInvite(viewState.bubbleCallInvite)
            setBubbleBotResponse(viewState.bubbleBotResponse)
            setBubbleDirectPaymentLayout(viewState.bubbleDirectPayment)
            setBubbleDirectPaymentLayout(viewState.bubbleDirectPayment)
            setBubblePodcastBoost(viewState.bubblePodcastBoost)
            setBubblePaidMessageReceivedDetailsLayout(
                viewState.bubblePaidMessageReceivedDetails,
                viewState.background
            )
            setBubblePaidMessageSentStatusLayout(viewState.bubblePaidMessageSentStatus)
            setBubbleReactionBoosts(viewState.bubbleReactionBoosts) { imageView, url ->
                lifecycleScope.launch(dispatchers.mainImmediate) {
                    imageLoader.load(imageView, url.value, imageLoaderDefaults)
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

@Suppress("NOTHING_TO_INLINE")
internal inline fun Long.toTimestamp(): String {
    val minutes = this / 1000 / 60
    val seconds = this / 1000 % 60

    return "${"%02d".format(minutes)}:${"%02d".format(seconds)}"
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
                    // ❓: Should we ever get here if the type isn't one we plan to
                    // render a unique message for?
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
    directPayment: LayoutState.Bubble.ContainerSecond.DirectPayment?
) {
    includeMessageHolderBubble.includeMessageTypeDirectPayment.apply {
        if (directPayment == null) {
            root.gone
        } else {
            root.visible

            imageViewDirectPaymentSent.goneIfFalse(directPayment.showSent)
            layoutConstraintDirectPaymentSentAmountLabels.goneIfFalse(directPayment.showSent)

            imageViewDirectPaymentReceived.goneIfFalse(directPayment.showReceived)
            layoutConstraintDirectPaymentReceivedAmountLabels.goneIfFalse(directPayment.showReceived)

            textViewSatsAmountReceived.text = directPayment.amount.asFormattedString()
            textViewSatsUnitLabelReceived.text = directPayment.unitLabel

            textViewSatsAmountSent.text = directPayment.amount.asFormattedString()
            textViewSatsUnitLabelSent.text = directPayment.unitLabel
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
                val avatarImageSpace = root
                    .context
                    .resources
                    .getDimensionPixelSize(R.dimen.message_holder_space_width_left)

                spaceMessageHolderLeft.updateLayoutParams {
                    width = avatarImageSpace
                }
                spaceMessageHolderRight.updateLayoutParams {
                    width = (holderWidth.value * BubbleBackground.SPACE_WIDTH_MULTIPLE).toInt() - (avatarImageSpace / 2)
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
    statusHeader: LayoutState.MessageStatusHeader?,
    holderJobs: ArrayList<Job>,
    dispatchers: CoroutineDispatchers,
    lifecycleScope: CoroutineScope,
    userColorsHelper: UserColorsHelper,
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
    message: LayoutState.Bubble.ContainerThird.Message?,
    onSphinxInteractionListener: SphinxUrlSpan.OnInteractionListener?
) {
    includeMessageHolderBubble.textViewMessageText.apply {
        if (message == null) {
            gone
        } else {
            visible
            text = message.text

            if (onSphinxInteractionListener != null) {
                SphinxLinkify.addLinks(this, SphinxLinkify.ALL, onSphinxInteractionListener)
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
) {
    includeMessageHolderBubble.apply {
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
    botResponse: LayoutState.Bubble.ContainerSecond.BotResponse?
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
    loadImage: (ImageView, String, MessageMedia?) -> Unit,
) {
    includeMessageHolderBubble.includeMessageTypeImageAttachment.apply {
        if (imageAttachment == null) {
            root.gone
        } else {
            root.visible

            if (imageAttachment.showPaidOverlay) {
                layoutConstraintPaidImageOverlay.visible
            } else {
                layoutConstraintPaidImageOverlay.gone

                loadImage(imageViewAttachmentImage, imageAttachment.url, imageAttachment.media)
            }
        }
    }
}

@MainThread
@Suppress("NOTHING_TO_INLINE")
internal inline fun LayoutMessageHolderBinding.setBubbleAudioAttachment(
    audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment?,
    loadAudio: (LayoutMessageTypeAttachmentAudioBinding, String, MessageMedia?) -> Unit,
) {
    includeMessageHolderBubble.includeMessageTypeAudioAttachment.apply {
        if (audioAttachment == null) {
            root.gone
        } else {
            root.visible
            progressBarAttachmentAudioFileLoading.visible
            textViewAttachmentPlayPauseButton.gone
            textViewAttachmentAudioFailure.gone

            loadAudio(
                this,
                audioAttachment.url,
                audioAttachment.media
            )
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
            imageViewReplyTextOverlay.gone

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

            textViewReplyMessageLabel.text = replyMessage.text
            textViewReplyMessageLabel.goneIfFalse(replyMessage.text.isNotEmpty())
        }
    }
}
