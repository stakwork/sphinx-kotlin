package chat.sphinx.chat_common.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.model.NodeDescriptor
import chat.sphinx.chat_common.model.TribeLink
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.isMessageSelected
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.audio.AudioMessageState
import chat.sphinx.chat_common.ui.viewstate.audio.AudioPlayState
import chat.sphinx.chat_common.ui.viewstate.messageholder.*
import chat.sphinx.chat_common.ui.viewstate.selected.SelectedMessageViewState
import chat.sphinx.chat_common.util.*
import chat.sphinx.concept_image_loader.*
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.resources.getString
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.asFormattedString
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MessageListAdapter<ARGS : NavArgs>(
    private val recyclerView: RecyclerView,
    private val headerBinding: LayoutChatHeaderBinding,
    private val headerPinBinding: LayoutChatPinedMessageHeaderBinding?,
    private val layoutManager: LinearLayoutManager,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: ChatViewModel<ARGS>,
    private val imageLoader: ImageLoader<ImageView>,
    private val userColorsHelper: UserColorsHelper,
    private val isThreadChat: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    DefaultLifecycleObserver,
    View.OnLayoutChangeListener
{

    companion object {
        private const val VIEW_TYPE_MESSAGE = 0
        private const val VIEW_TYPE_THREAD_HEADER = 1
    }

    interface OnRowLayoutListener {
        fun onRowHeightChanged()
    }

    private val onRowLayoutListener: OnRowLayoutListener = object: OnRowLayoutListener {
        override fun onRowHeightChanged() {
            val lastVisibleItemPosition = (recyclerView.layoutManager as? LinearLayoutManager)?.findLastVisibleItemPosition()
            val itemsCount = (recyclerView.layoutManager?.itemCount ?: 0)
            val isScrolledAtLastRow = lastVisibleItemPosition == (itemsCount - 1)

            if (isScrolledAtLastRow) {
                forceScrollToBottom()
            }
        }
    }

    private inner class Diff(
        private val oldList: List<MessageHolderViewState>,
        private val newList: List<MessageHolderViewState>
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                oldList[oldItemPosition].message?.id == newList[newItemPosition].message?.id
            } catch (e: IndexOutOfBoundsException) {
                false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                val old = oldList[oldItemPosition]
                val new = newList[newItemPosition]

                when {
                    old is MessageHolderViewState.Received && new is MessageHolderViewState.Received -> {
                        old.background                         == new.background                   &&
                        old.message                            == new.message                      &&
                        old.invoiceLinesHolderViewState        == new.invoiceLinesHolderViewState  &&
                        old.message?.thread                    == new.message?.thread
                    }
                    old is MessageHolderViewState.Sent && new is MessageHolderViewState.Sent -> {
                        old.background                         == new.background                    &&
                        old.message                            == new.message                       &&
                        old.invoiceLinesHolderViewState        == new.invoiceLinesHolderViewState   &&
                        old.isPinned                           == new.isPinned                      &&
                        old.message?.thread                    == new.message?.thread
                    }
                    else -> {
                        false
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                false
            }
        }
    }

    private val messages = ArrayList<MessageHolderViewState>(viewModel.messageHolderViewStateFlow.value)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.main) {
            viewModel.messageHolderViewStateFlow.collect { list ->

                // Delay added to ensure navigation animation is done
                if (messages.isEmpty()) {
                    messages.addAll(list)
                    notifyDataSetChanged()
                    scrollToUnseenSeparatorOrBottom(list)
                } else {
                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(
                            Diff(messages, list)
                        )
                    }.let { result ->
                        scrollToPreviousPosition(callback = {
                            messages.clear()
                            messages.addAll(list)
                            result.dispatchUpdatesTo(this@MessageListAdapter)
                        })
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages.getOrNull(position)) {
            is MessageHolderViewState.ThreadHeader -> VIEW_TYPE_THREAD_HEADER
            else -> VIEW_TYPE_MESSAGE
        }
    }

    private fun scrollToUnseenSeparatorOrBottom(messageHolders: List<MessageHolderViewState>) {
        for ((index, message) in messageHolders.withIndex()) {
            (message as? MessageHolderViewState.Separator)?.let {
                if (it.messageHolderType.isUnseenSeparatorHolder()) {
                    (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, recyclerView.measuredHeight / 4)
                    return
                }
            }
        }

        recyclerView.layoutManager?.scrollToPosition(
            messageHolders.size
        )
    }

    fun scrollToBottomIfNeeded(
        callback: (() -> Unit)? = null,
        replyingToMessage: Boolean = false,
        itemsDiff: Int = 0
    ) {
        val lastVisibleItemPositionBeforeDispatch = layoutManager.findLastVisibleItemPosition()
        val listSizeBeforeDispatch = messages.size

        if (callback != null) {
            callback()
        }

        val listSizeAfterDispatch = messages.size
        val lastItemPosition = messages.size - 1

        if (
            (!viewModel.isMessageSelected() || replyingToMessage)                    &&
            listSizeAfterDispatch >= listSizeBeforeDispatch                          &&
            recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE               &&
            lastVisibleItemPositionBeforeDispatch + itemsDiff >= lastItemPosition
        ) {
            recyclerView.scrollToPosition(listSizeAfterDispatch)
        }
    }

    private fun scrollToPreviousPosition(
        callback: (() -> Unit)? = null,
    ) {
        val lastVisibleItemPositionBeforeDispatch = layoutManager.findLastVisibleItemPosition()
        val listSizeBeforeDispatch = messages.size
        val diffToBottom = listSizeBeforeDispatch - lastVisibleItemPositionBeforeDispatch

        if (callback != null) {
            callback()
        }

        val listSizeAfterDispatch = messages.size
        recyclerView.scrollToPosition(listSizeAfterDispatch - diffToBottom)
    }

    fun forceScrollToBottom() {
        recyclerView.layoutManager?.smoothScrollToPosition(recyclerView, null, messages.size);
    }

    fun highlightAndScrollToSearchResult(
        message: Message,
        previousMessage: Message?,
        searchTerm: String
    ) {
        var previousMessageUpdated = (previousMessage == null)
        var indexToScroll: Int? = null

        for ((index, messageHolderVS) in messages.withIndex()) {
            if (messageHolderVS.message?.id == previousMessage?.id && !previousMessageUpdated) {

                (messageHolderVS as? MessageHolderViewState.Sent)?.let {
                    it.highlightedText = null
                } ?: (messageHolderVS as? MessageHolderViewState.Received)?.let {
                    it.highlightedText = null
                }

                notifyItemChanged(index)

                previousMessageUpdated = true
            }

            if (messageHolderVS.message?.id == message.id && indexToScroll == null) {

                (messageHolderVS as? MessageHolderViewState.Sent)?.let {
                    it.highlightedText = searchTerm
                } ?: (messageHolderVS as? MessageHolderViewState.Received)?.let {
                    it.highlightedText = searchTerm
                }

                notifyItemChanged(index)

                indexToScroll = index
            }

            if (previousMessageUpdated) {
                indexToScroll?.let {
                    recyclerView.scrollToPosition(it)
                    return
                }
            }
        }
    }

    fun resetHighlighted() {
        for ((index, messageHolderVS) in messages.withIndex()) {
            if (messageHolderVS.highlightedText != null) {
                messageHolderVS.highlightedText = null
                notifyItemChanged(index)
            }
        }
    }

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        if (bottom != oldBottom) {
            val lastPosition = messages.size - 1
            if (
                !viewModel.isMessageSelected()                              &&
                recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE  &&
                layoutManager.findLastVisibleItemPosition() == lastPosition
            ) {
                recyclerView.scrollToPosition(lastPosition)
            }

        }
    }

    init {
        recyclerView.addOnLayoutChangeListener(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        recyclerView.removeOnLayoutChangeListener(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MESSAGE -> {
                val binding = LayoutMessageHolderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                MessageViewHolder(binding)
            }
            VIEW_TYPE_THREAD_HEADER -> {
                val binding = LayoutThreadMessageHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ThreadHeaderViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            VIEW_TYPE_THREAD_HEADER == getItemViewType(position) -> {
                (holder as MessageListAdapter<ARGS>.ThreadHeaderViewHolder).bind(position)
            }
            else -> {
                (holder as MessageListAdapter<ARGS>.MessageViewHolder).bind(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    private val recyclerViewWidth: Px by lazy(LazyThreadSafetyMode.NONE) {
        Px(recyclerView.measuredWidth.toFloat())
    }

    private val headerHeight: Px by lazy(LazyThreadSafetyMode.NONE) {
        Px(headerBinding.root.measuredHeight.toFloat())
    }

    private val screenHeight: Px by lazy(LazyThreadSafetyMode.NONE) {
        Px(recyclerView.rootView.measuredHeight.toFloat())
    }

    private val pinedMessageHeader: Px
    get() {
        return headerPinBinding?.let {
            if (headerPinBinding.root.isVisible) {
                Px(headerPinBinding.root.measuredHeight.toFloat())
            } else {
                Px(0f)
            }
        } ?: Px(0f)
    }

    inner class MessageViewHolder(
        private val binding: LayoutMessageHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(15)
        private val disposables: ArrayList<Disposable> = ArrayList(4)
        private var currentViewState: MessageHolderViewState? = null

        private val onSphinxInteractionListener: SphinxUrlSpan.OnInteractionListener
        init {
            binding.includeMessageHolderBubble.apply {

                val linkPreviewClickListener = View.OnClickListener {
                    currentViewState?.messageLinkPreview?.let { preview ->
                        if (preview is NodeDescriptor) {
                            viewModel.handleContactTribeLinks(preview.nodeDescriptor.value)
                        } else if (preview is TribeLink) {
                            viewModel.handleContactTribeLinks(preview.tribeJoinLink.value)
                        }
                    }
                }

                val selectedMessageLongClickListener = OnLongClickListener {
                    SelectedMessageViewState.SelectedMessage.instantiate(
                        messageHolderViewState = currentViewState,
                        holderYPosTop = Px(binding.root.y + binding.includeMessageHolderBubble.root.y),
                        holderHeight = Px(binding.root.measuredHeight.toFloat()),
                        holderWidth = Px(binding.root.measuredWidth.toFloat()),
                        bubbleXPosStart = Px(root.x),
                        bubbleWidth = Px(root.measuredWidth.toFloat()),
                        bubbleHeight = Px(root.measuredHeight.toFloat()),
                        headerHeight = headerHeight,
                        recyclerViewWidth = recyclerViewWidth,
                        screenHeight = screenHeight,
                        pinedHeaderHeight = pinedMessageHeader
                    ).let { vs ->
                        viewModel.updateSelectedMessageViewState(vs)
                    }
                    true
                }

                onSphinxInteractionListener = object: SphinxUrlSpan.OnInteractionListener(
                    selectedMessageLongClickListener
                ) {
                    override fun onClick(url: String?) {
                        viewModel.handleContactTribeLinks(url)
                    }
                }

                root.setOnLongClickListener(onSphinxInteractionListener)

                SphinxLinkify.addLinks(textViewMessageText, SphinxLinkify.ALL, binding.root.context, onSphinxInteractionListener)

                textViewMessageText.setOnLongClickListener(onSphinxInteractionListener)

                includeMessageTypeBotResponse.webViewMessageTypeBotResponse.setOnLongClickListener(onSphinxInteractionListener)

                includeMessageLinkPreviewContact.apply contact@ {
                    root.setOnLongClickListener(selectedMessageLongClickListener)
                    root.setOnClickListener(linkPreviewClickListener)
                }

                includeMessageLinkPreviewTribe.apply tribe@ {
                    root.setOnLongClickListener(selectedMessageLongClickListener)
                    root.setOnClickListener(linkPreviewClickListener)
                }

                includeMessageTypeCallInvite.let { holder ->
                    holder.layoutConstraintCallInviteJoinByAudio.setOnClickListener {
                        currentViewState?.message?.let { nnMessage ->
                            joinCall(nnMessage, true)
                        }
                    }

                    holder.layoutConstraintCallInviteJoinByVideo.setOnClickListener {
                        currentViewState?.message?.let { nnMessage ->
                            joinCall(nnMessage, false)
                        }
                    }

                    holder.buttonCallInviteCopyLink.setOnClickListener {
                        currentViewState?.message?.let { nnMessage ->
                            viewModel.copyCallLink(nnMessage)
                        }
                    }
                }

                includeMessageTypeImageAttachment.apply {
                    imageViewAttachmentImage.setOnClickListener {
                        currentViewState?.message?.let { message ->
                            viewModel.showAttachmentImageFullscreen(message)
                        }
                    }
                    imageViewAttachmentImage.setOnLongClickListener(selectedMessageLongClickListener)
                }

                includeMessageTypeVideoAttachment.apply {
                    textViewAttachmentPlayButton.setOnClickListener {
                        currentViewState?.message?.let { message ->
                            viewModel.goToFullscreenVideo(message.id)
                        }
                    }
                }

                includeMessageTypeFileAttachment.apply {
                    buttonAttachmentFileDownload.setOnClickListener {
                        currentViewState?.message?.let { message ->
                            viewModel.saveFile(message, null)
                        }
                    }
                    layoutConstraintAttachmentFileMainInfoGroup.setOnClickListener {
                        currentViewState?.message?.let { message ->
                            viewModel.showAttachmentPdfFullscreen(message, 0)
                        }
                    }
                    layoutConstraintAttachmentFileMainInfoGroup.setOnLongClickListener(selectedMessageLongClickListener)
                }

                includeLayoutMessageThread.apply {
                    root.setOnClickListener {
                        currentViewState?.message?.let { message ->
                            message.uuid?.let { nnUUID -> viewModel.navigateToChatThread(nnUUID) }
                        }
                    }
                }

                includePaidMessageReceivedDetailsHolder.apply {
                    buttonPayAttachment.setOnClickListener {
                        currentViewState?.message?.let { message ->
                            viewModel.payAttachment(message)
                        }
                    }
                    buttonPayAttachment.setOnLongClickListener(selectedMessageLongClickListener)
                }

                includeMessageTypeInvoice.apply {
                    buttonPay.setOnClickListener {
                        currentViewState?.message?.let { message ->
                            viewModel.payInvoice(message)
                        }
                    }
                }

                includeMessageTypeAudioAttachment.apply {
                    textViewAttachmentPlayPauseButton.setOnClickListener {
                        viewModel.audioPlayerController.togglePlayPause(
                            currentViewState?.bubbleAudioAttachment
                        )
                    }
                    seekBarAttachmentAudio.setOnTouchListener { _, _ -> true }
                }

                includeMessageTypePodcastClip.apply {
                    layoutConstraintPlayPauseButton.setOnClickListener {
                        viewModel.audioPlayerController.togglePlayPause(
                            currentViewState?.bubblePodcastClip
                        )
                    }
                    seekBarPodcastClip.setOnTouchListener { _, _ -> true }
                    seekBarPodcastClip.setPadding(0,0,0,0)
                }
            }

            binding.includeMessageTypeGroupActionHolder.let { holder ->
                holder.includeMessageTypeGroupActionJoinRequest.apply {
                    textViewGroupActionJoinRequestAcceptAction.setOnClickListener {
                        currentViewState?.message?.let { nnMessage ->

                            if (nnMessage.type is MessageType.GroupAction.MemberRequest) {
                                processMemberRequest(
                                    nnMessage.sender,
                                    nnMessage.id,
                                    MessageType.GroupAction.MemberApprove
                                )
                            }
                        }
                    }

                    textViewGroupActionJoinRequestRejectAction.setOnClickListener {
                        currentViewState?.message?.let { nnMessage ->

                            if (nnMessage.type is MessageType.GroupAction.MemberRequest) {
                                processMemberRequest(
                                    nnMessage.sender,
                                    nnMessage.id,
                                    MessageType.GroupAction.MemberReject
                                )
                            }
                        }
                    }
                }

                holder.includeMessageTypeGroupActionMemberRemoval.apply {
                    textViewGroupActionMemberRemovalDeleteGroup.setOnClickListener {
                        deleteTribe()
                    }
                }
            }

            binding.includeMessageHolderChatImageInitialHolder.root.setOnClickListener {
                currentViewState?.message?.let { nnMessage ->
                    viewModel.onSmallProfileImageClick(nnMessage)
                }
            }
        }

        private fun processMemberRequest(contactId: ContactId, messageId: MessageId, type: MessageType.GroupAction) {
            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                binding.includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionJoinRequest.apply {
                    layoutConstraintGroupActionJoinRequestProgressBarContainer.visible

                    viewModel.processMemberRequest(
                        contactId,
                        messageId,
                        type
                    )

                    layoutConstraintGroupActionJoinRequestProgressBarContainer.gone
                }
            }.let { job ->
                holderJobs.add(job)
            }
        }

        private fun deleteTribe() {
            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                binding.includeMessageTypeGroupActionHolder.includeMessageTypeGroupActionMemberRemoval.apply {
                    layoutConstraintGroupActionMemberRemovalProgressBarContainer.visible

                    viewModel.deleteTribe()

                    layoutConstraintGroupActionMemberRemovalProgressBarContainer.gone
                }
            }.let { job ->
                holderJobs.add(job)
            }
        }

        private fun joinCall(message: Message, audioOnly: Boolean) {
            viewModel.joinCall(message, audioOnly)
        }

        fun bind(position: Int) {
            val viewState = messages.elementAtOrNull(position).also { currentViewState = it } ?: return
            audioAttachmentJob?.cancel()

            binding.setView(
                lifecycleOwner.lifecycleScope,
                holderJobs,
                disposables,
                viewModel.dispatchers,
                viewModel.audioPlayerController,
                imageLoader,
                viewModel.memeServerTokenHandler,
                recyclerViewWidth,
                viewState,
                userColorsHelper,
                onSphinxInteractionListener,
                onRowLayoutListener
            )

            observeAudioAttachmentState()
        }

        private var audioAttachmentJob: Job? = null
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)

            audioAttachmentJob?.let { job ->
                if (!job.isActive) {
                    observeAudioAttachmentState()
                }
            }
        }

        private fun observeAudioAttachmentState() {
            currentViewState?.bubbleAudioAttachment?.let { audioAttachment ->
                if (audioAttachment is LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable) {
                    audioAttachmentJob?.cancel()
                    audioAttachmentJob = onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        viewModel.audioPlayerController.getAudioState(audioAttachment)?.collect { audioState ->
                            binding.includeMessageHolderBubble
                                .includeMessageTypeAudioAttachment
                                .setAudioAttachmentLayoutForState(audioState)
                        }
                    }
                }
            }

            currentViewState?.bubblePodcastClip?.let { podcastClipViewState ->
                audioAttachmentJob?.cancel()
                audioAttachmentJob = onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.audioPlayerController.getAudioState(podcastClipViewState)?.collect { audioState ->
                        binding.includeMessageHolderBubble
                            .includeMessageTypePodcastClip
                            .setPodcastClipLayoutForState(audioState)
                    }
                }
            }
        }

        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }

    }

    inner class ThreadHeaderViewHolder(
        private val binding: LayoutThreadMessageHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {
        private var threadHeaderViewState: MessageHolderViewState.ThreadHeader? = null

        private var audioAttachmentJob: Job? = null
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)

            audioAttachmentJob?.let { job ->
                if (!job.isActive) {
                    observeAudioAttachmentState()
                }
            }
        }

        init {
            binding.apply {

                constraintShowMoreContainer.setOnClickListener {
                    viewModel.toggleThreadDescriptionExpanded()
                }

                includeMessageTypeImageAttachment.imageViewAttachmentImage.setOnClickListener {
                    threadHeaderViewState?.message?.let { message ->
                        viewModel.showAttachmentImageFullscreen(message)
                    }
                }

                includeMessageTypeVideoAttachment.apply {
                    textViewAttachmentPlayButton.setOnClickListener {
                        threadHeaderViewState?.message?.let { message ->
                            viewModel.goToFullscreenVideo(message.id)
                        }
                    }
                }
                includeMessageTypeFileAttachment.apply {
                    buttonAttachmentFileDownload.setOnClickListener {
                        threadHeaderViewState?.message?.let { message ->
                            viewModel.saveFile(message, null)
                        }
                    }
                    layoutConstraintAttachmentFileMainInfoGroup.setOnClickListener {
                        threadHeaderViewState?.message?.let { message ->
                            viewModel.showAttachmentPdfFullscreen(message, 0)
                        }
                    }
                }

                includeMessageTypeAudioAttachment.apply {
                    textViewAttachmentPlayPauseButton.setOnClickListener {
                        threadHeaderViewState?.bubbleAudioAttachment?.let { bubbleAudioAttachment ->
                            viewModel.audioPlayerController.togglePlayPause(bubbleAudioAttachment)
                        }
                    }
                    seekBarAttachmentAudio.setOnTouchListener { _, _ -> true }
                }

                includeMessageTypeFileAttachment.root.setBackgroundResource(R.drawable.background_thread_file_attachment)
            }
        }

        private fun observeAudioAttachmentState() {
            threadHeaderViewState?.bubbleAudioAttachment?.let { audioAttachment ->
                if (audioAttachment is LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable) {
                    audioAttachmentJob?.cancel()
                    audioAttachmentJob = onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        viewModel.audioPlayerController.getAudioState(audioAttachment)
                            ?.collect { audioState ->
                                binding.includeMessageTypeAudioAttachment.setAudioAttachmentLayoutForState(audioState)
                            }
                    }
                }
            }
        }

        fun bind(position: Int) {
            val threadHeader = messages.getOrNull(position) as MessageHolderViewState.ThreadHeader
            threadHeaderViewState = threadHeader

            binding.apply {
                root.visible

                val senderInfo: Triple<PhotoUrl?, ContactAlias?, String>? = if (threadHeader.message != null) {
                    threadHeader.messageSenderInfo(threadHeader.message!!)
                } else {
                    null
                }

                textViewContactMessageHeaderName.text = senderInfo?.second?.value ?: ""
                textViewThreadDate.text = threadHeader.timestamp
                textViewThreadMessageContent.text = threadHeader.bubbleMessage?.text ?: ""
                textViewThreadMessageContent.goneIfFalse(threadHeader.bubbleMessage?.text?.isNotEmpty() == true)

                textViewThreadDate.post(Runnable {
                    val linesCount: Int = textViewThreadDate.lineCount

                    if (linesCount <= 12) {
                        textViewShowMore.gone
                    } else {
                        if (threadHeader.isExpanded) {
                            textViewThreadMessageContent.maxLines = Int.MAX_VALUE
                            textViewShowMore.text =
                                getString(R.string.episode_description_show_less)
                        } else {
                            textViewThreadMessageContent.maxLines = 12
                            textViewShowMore.text =
                                getString(R.string.episode_description_show_more)
                        }
                    }
                })

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {

                    binding.layoutContactInitialHolder.apply {
                        senderInfo?.third?.let {
                            textViewInitialsName.visible
                            imageViewChatPicture.gone

                            textViewInitialsName.apply {
                                text = (senderInfo?.second?.value ?: "")?.getInitials()

                                setBackgroundRandomColor(
                                    R.drawable.chat_initials_circle,
                                    Color.parseColor(
                                        userColorsHelper.getHexCodeForKey(
                                            it,
                                            root.context.getRandomHexCode(),
                                        )
                                    ),
                                )
                            }
                        }

                        binding.constraintMediaThreadContainer.gone
                        binding.includeMessageTypeFileAttachment.root.gone
                        binding.includeMessageTypeVideoAttachment.root.gone
                        binding.includeMessageTypeImageAttachment.root.gone

                        binding.includeMessageTypeImageAttachment.apply {
                            threadHeader.bubbleImageAttachment?.let {
                                binding.constraintMediaThreadContainer.visible
                                root.visible
                                layoutConstraintPaidImageOverlay.gone
                                loadingImageProgressContainer.visible
                                imageViewAttachmentImage.visible

                                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                    imageViewAttachmentImage.scaleType = ImageView.ScaleType.CENTER_CROP

                                    it.media?.localFile?.let {
                                        imageLoader.load(
                                            imageViewAttachmentImage,
                                            it,
                                            ImageLoaderOptions.Builder().build()
                                        )
                                    } ?: it?.url?.let {
                                        imageLoader.load(
                                            imageViewAttachmentImage,
                                            it,
                                            ImageLoaderOptions.Builder().build()
                                        )
                                    }
                                }
                            }
                        }

                        binding.includeMessageTypeVideoAttachment.apply {
                            (threadHeader.bubbleVideoAttachment as? LayoutState.Bubble.ContainerSecond.VideoAttachment.FileAvailable)?.let {
                                VideoThumbnailUtil.loadThumbnail(it.file)?.let { thumbnail ->
                                    binding.constraintMediaThreadContainer.visible
                                    root.visible

                                    imageViewAttachmentThumbnail.setImageBitmap(thumbnail)
                                    imageViewAttachmentThumbnail.visible
                                    layoutConstraintVideoPlayButton.visible
                                }
                            }
                        }

                        binding.includeMessageTypeFileAttachment.apply {
                            (threadHeader.bubbleFileAttachment as? LayoutState.Bubble.ContainerSecond.FileAttachment.FileAvailable)?.let { fileAttachment ->
                                binding.constraintMediaThreadContainer.visible
                                root.visible
                                progressBarAttachmentFileDownload.gone
                                buttonAttachmentFileDownload.visible

                                textViewAttachmentFileIcon.text =
                                    if (fileAttachment.isPdf) {
                                        getString(chat.sphinx.chat_common.R.string.material_icon_name_file_pdf)
                                    } else {
                                        getString(chat.sphinx.chat_common.R.string.material_icon_name_file_attachment)
                                    }

                                textViewAttachmentFileName.text =
                                    fileAttachment.fileName?.value ?: "File.txt"

                                textViewAttachmentFileSize.text =
                                    if (fileAttachment.isPdf) {
                                        if (fileAttachment.pageCount > 1) {
                                            "${fileAttachment.pageCount} ${getString(
                                                    chat.sphinx.chat_common.R.string.pdf_pages
                                                )}"
                                        } else {
                                            "${fileAttachment.pageCount} ${getString(
                                                    chat.sphinx.chat_common.R.string.pdf_page
                                                )}"
                                        }
                                    } else {
                                        fileAttachment.fileSize.asFormattedString()
                                    }
                            }
                        }

                        binding.includeMessageTypeAudioAttachment.apply {
                            (threadHeader.bubbleAudioAttachment as? LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable)?.let { audioAttachment ->
                                binding.constraintMediaThreadContainer.visible
                                root.visible
                                includeMessageTypeAudioAttachment.root.setBackgroundResource(R.drawable.background_thread_file_attachment)

                                onStopSupervisor.scope.launch(viewModel.io) {
                                    viewModel.audioPlayerController.getAudioState(audioAttachment)?.value?.let { state ->
                                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                            setAudioAttachmentLayoutForState(state)
                                        }
                                    } ?: run {
                                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
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
                                }
                            }
                        }

                        senderInfo?.first?.let { photoUrl ->
                            textViewInitialsName.gone
                            imageViewChatPicture.visible

                            imageLoader.load(
                                layoutContactInitialHolder.imageViewChatPicture,
                                photoUrl.value,
                                ImageLoaderOptions.Builder()
                                    .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                    .transformation(Transformation.CircleCrop)
                                    .build()
                            )
                        }
                    }
                }
            }
            observeAudioAttachmentState()
        }

        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}
