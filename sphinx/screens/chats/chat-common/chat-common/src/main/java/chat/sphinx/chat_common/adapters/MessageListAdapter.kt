package chat.sphinx.chat_common.adapters

import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.model.NodeDescriptor
import chat.sphinx.chat_common.model.TribeLink
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.isMessageSelected
import chat.sphinx.chat_common.ui.viewstate.messageholder.*
import chat.sphinx.chat_common.ui.viewstate.selected.SelectedMessageViewState
import chat.sphinx.chat_common.util.*
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.notifyAll

internal class MessageListAdapter<ARGS : NavArgs>(
    private val recyclerView: RecyclerView,
    private val headerBinding: LayoutChatHeaderBinding,
    private val layoutManager: LinearLayoutManager,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: ChatViewModel<ARGS>,
    private val imageLoader: ImageLoader<ImageView>,
    private val userColorsHelper: UserColorsHelper,
) : RecyclerView.Adapter<MessageListAdapter<ARGS>.MessageViewHolder>(),
    DefaultLifecycleObserver,
    View.OnLayoutChangeListener
{

    interface OnRowLayoutListener {
        fun onRowHeightChanged()
    }

    private val onRowLayoutListener: OnRowLayoutListener = object: OnRowLayoutListener {
        override fun onRowHeightChanged() {
            val lastVisibleItemPosition = (recyclerView.layoutManager as? LinearLayoutManager)?.findLastVisibleItemPosition()
            val itemsCount = (recyclerView.layoutManager?.itemCount ?: 0)
            val isScrolledToBottom = lastVisibleItemPosition == (itemsCount - 1)

            if (isScrolledToBottom) {
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
                oldList[oldItemPosition].message.id == newList[newItemPosition].message.id
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
                        old.background                         == new.background        &&
                        old.message                            == new.message           &&
                        old.invoiceLinesHolderViewState        == new.invoiceLinesHolderViewState
                    }
                    old is MessageHolderViewState.Sent && new is MessageHolderViewState.Sent -> {
                        old.background                         == new.background        &&
                        old.message                            == new.message           &&
                        old.invoiceLinesHolderViewState        == new.invoiceLinesHolderViewState
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

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.messageHolderViewStateFlow.collect { list ->
                if (messages.isEmpty()) {
                    messages.addAll(list)
                    notifyDataSetChanged()
                    recyclerView.layoutManager?.scrollToPosition(messages.size)
                } else {
                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(
                            Diff(messages, list)
                        )
                    }.let { result ->
                        scrollToBottomIfNeeded(callback = {
                            messages.clear()
                            messages.addAll(list)
                            result.dispatchUpdatesTo(this@MessageListAdapter)
                        })
                    }
                }
            }
        }
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

    fun forceScrollToBottom() {
        recyclerView.scrollToPosition(messages.size)
    }

    fun highlightAndScrollToSearchResult(
        message: Message,
        previousMessage: Message?,
        searchTerm: String
    ) {
        var previousMessageUpdated = (previousMessage == null)
        var indexToScroll: Int? = null

        for ((index, messageHolderVS) in messages.withIndex()) {
            if (messageHolderVS.message.id == previousMessage?.id && !previousMessageUpdated) {

                (messageHolderVS as? MessageHolderViewState.Sent)?.let {
                    it.highlightedText = null
                } ?: (messageHolderVS as? MessageHolderViewState.Received)?.let {
                    it.highlightedText = null
                }

                notifyItemChanged(index)

                previousMessageUpdated = true
            }

            if (messageHolderVS.message.id == message.id && indexToScroll == null) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = LayoutMessageHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(position)
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
                        screenHeight = screenHeight
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

                SphinxLinkify.addLinks(textViewMessageText, SphinxLinkify.ALL, onSphinxInteractionListener)
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
                    viewModel.showMemberPopup(nnMessage)
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

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}
