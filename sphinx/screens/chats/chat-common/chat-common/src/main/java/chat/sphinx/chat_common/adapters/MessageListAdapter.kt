package chat.sphinx.chat_common.adapters

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
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.isMessageSelected
import chat.sphinx.chat_common.ui.viewstate.messageholder.*
import chat.sphinx.chat_common.ui.viewstate.selected.SelectedMessageViewState
import chat.sphinx.chat_common.util.*
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.join_tribe.R
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MessageListAdapter<ARGS : NavArgs>(
    private val recyclerView: RecyclerView,
    private val headerBinding: LayoutChatHeaderBinding,
    private val layoutManager: LinearLayoutManager,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: ChatViewModel<ARGS>,
    private val imageLoader: ImageLoader<ImageView>,
) : RecyclerView.Adapter<MessageListAdapter<ARGS>.MessageViewHolder>(),
    DefaultLifecycleObserver,
    View.OnLayoutChangeListener
//    SphinxUrlSpan.PreviewHandler
{

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
                        old.background == new.background        &&
                        old.message    == new.message
                    }
                    old is MessageHolderViewState.Sent && new is MessageHolderViewState.Sent -> {
                        old.background == new.background        &&
                        old.message    == new.message
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
                    recyclerView.layoutManager?.scrollToPosition(messages.size - 1)
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
        callback: () -> Unit,
        replyingToMessage: Boolean = false
    ) {
        val lastVisibleItemPositionBeforeDispatch = layoutManager.findLastVisibleItemPosition()
        val listSizeBeforeDispatch = messages.size - 1

        callback()

        val listSizeAfterDispatch = messages.size - 1

        if (
            (!viewModel.isMessageSelected() || replyingToMessage)           &&
            listSizeAfterDispatch >= listSizeBeforeDispatch                 &&
            recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE      &&
            lastVisibleItemPositionBeforeDispatch == listSizeBeforeDispatch
        ) {
            recyclerView.scrollToPosition(listSizeAfterDispatch)
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
    ): RecyclerView.ViewHolder(binding.root) {

        private val holderJobs: ArrayList<Job> = ArrayList(5)
        private val disposables: ArrayList<Disposable> = ArrayList(3)
        private var currentViewState: MessageHolderViewState? = null

        private val selectedMessageLongClickListener: OnLongClickListener

        private val onSphinxInteractionListener: SphinxUrlSpan.OnInteractionListener

        init {
            binding.includeMessageHolderBubble.apply {
                selectedMessageLongClickListener = OnLongClickListener { v ->
                    SelectedMessageViewState.SelectedMessage.instantiate(
                        messageHolderViewState = currentViewState,
                        holderYPosTop = Px(binding.root.y),
                        holderHeight = Px(binding.root.measuredHeight.toFloat()),
                        holderWidth = Px(binding.root.measuredWidth.toFloat()),
                        bubbleXPosStart = Px(v.x),
                        bubbleWidth = Px(v.measuredWidth.toFloat()),
                        bubbleHeight = Px(v.measuredHeight.toFloat()),
                        headerHeight = headerHeight,
                        statusHeaderHeight = Px(binding.includeMessageStatusHeader.root.measuredHeight.toFloat()),
                        recyclerViewWidth = recyclerViewWidth,
                        screenHeight = screenHeight,
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
            }

            binding.includeMessageHolderBubble.includeMessageTypeCallInvite.let { holder ->
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
//
//             TODO: Consider loading URL Previews here...
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

            binding.setView(
                lifecycleOwner.lifecycleScope,
                holderJobs,
                disposables,
                viewModel.dispatchers,
                imageLoader,
                viewModel.imageLoaderDefaults,
                viewModel.memeServerTokenHandler,
                recyclerViewWidth,
                viewState,
                onSphinxInteractionListener,
//                this@MessageListAdapter
            )

        }

    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

//    override fun populateTribePreview(
//        tribeJoinLink: TribeJoinLink,
//        layoutMessageLinkPreviewTribeBinding: LayoutMessageLinkPreviewTribeBinding
//    ) {
//        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//            // set progress thingy...
//            viewModel.getChatTribe(tribeJoinLink).collectLatest { chat ->
//                if (chat != null) {
//                    chat.name?.value?.let {
//                        populateTribePreview(
//                            name = it,
//                            null,
//                            img = chat.photoUrl?.value,
//                            isTribeMember = true,
//                            tribeJoinLink,
//                            layoutMessageLinkPreviewTribeBinding
//                        )
//                    }
//                } else {
//                    viewModel.getTribeInfo(tribeJoinLink).collect { loadResponse ->
//                        when (loadResponse) {
//                            is LoadResponse.Loading -> {}
//                            is Response.Error -> {
//                                layoutMessageLinkPreviewTribeBinding.root.gone
//                            }
//                            is Response.Success -> {
//                                val tribeInfo = loadResponse.value
//
//                                populateTribePreview(
//                                    tribeInfo.name,
//                                    tribeInfo.description,
//                                    tribeInfo.img,
//                                    isTribeMember = false,
//                                    tribeJoinLink,
//                                    layoutMessageLinkPreviewTribeBinding
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//
//        }
//    }
//
//    private fun populateTribePreview(
//        name: String,
//        description: String?,
//        img: String?,
//        isTribeMember: Boolean,
//        tribeJoinLink: TribeJoinLink,
//        layoutMessageLinkPreviewTribeBinding: LayoutMessageLinkPreviewTribeBinding
//    ) {
//        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//            layoutMessageLinkPreviewTribeBinding.apply {
//                textViewMessageLinkPreviewTribeNameLabel.text = name
//                if (description != null) {
//                    textViewMessageLinkPreviewTribeDescription.text = description
//                } else {
//                    textViewMessageLinkPreviewTribeDescription.gone
//                }
//
//                if (isTribeMember) {
//                    // Don't show the banner when we are a member of the tribe
//                    textViewMessageLinkPreviewTribeSeeBanner.gone
//                } else {
//                    textViewMessageLinkPreviewTribeSeeBanner.visible
//                }
//
//                img?.let {
//                    imageLoader.load(
//                        imageViewMessageLinkPreviewTribe,
//                        it,
//                        ImageLoaderOptions.Builder()
//                            .placeholderResId(R.drawable.ic_tribe_placeholder)
//                            .transformation(Transformation.CircleCrop)
//                            .build()
//                    )
//                }
//                textViewMessageLinkPreviewTribeSeeBanner.setOnClickListener {
//                    viewModel.handleContactTribeLinks(tribeJoinLink.value)
//                }
//                root.setOnClickListener {
//                    viewModel.handleContactTribeLinks(tribeJoinLink.value)
//                }
//                root.visible
//            }
//        }
//    }
//
//    override fun populateContactPreview(
//        lightningNodePubKey: LightningNodePubKey,
//        layoutMessageLinkPreviewContactBinding: LayoutMessageLinkPreviewContactBinding
//    ) {
//        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//            viewModel.getContact(lightningNodePubKey).collect { contact ->
//                layoutMessageLinkPreviewContactBinding.apply {
//                    textViewMessageLinkPreviewContactPubkey.text = lightningNodePubKey.value
//                    if (contact != null) {
//                        textViewMessageLinkPreviewNewContactLabel.text = contact.alias?.value
//                        textViewMessageLinkPreviewAddContactBanner.gone
//                        contact.photoUrl?.let {
//                            imageLoader.load(
//                                imageViewMessageLinkPreviewContactAvatar,
//                                it.value,
//                                ImageLoaderOptions.Builder()
//                                    .placeholderResId(R.drawable.ic_tribe_placeholder)
//                                    .transformation(Transformation.CircleCrop)
//                                    .build()
//                            )
//                        }
//                    } else {
//                        textViewMessageLinkPreviewAddContactBanner.setOnClickListener {
//                            viewModel.handleContactTribeLinks(lightningNodePubKey.value)
//                        }
//                    }
//                    root.setOnClickListener {
//                        viewModel.handleContactTribeLinks(lightningNodePubKey.value)
//                    }
//                    root.visible
//                }
//
//            }
//        }
//    }
//
//    override fun populateUrlPreview(
//        url: String,
//        layoutMessageLinkPreviewUrlBinding: LayoutMessageLinkPreviewUrlBinding
//    ) {
//        layoutMessageLinkPreviewUrlBinding.root.visible
//        layoutMessageLinkPreviewUrlBinding.constraintLayoutUrlLinkPreview.gone
//
//        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//            viewModel.getUrlMetaData(url).collectLatest { loadResponse ->
//                when (loadResponse) {
//                    LoadResponse.Loading -> {
//                        layoutMessageLinkPreviewUrlBinding.progressBarLinkPreview.visible
//                    }
//                    is Response.Error -> {
//                        layoutMessageLinkPreviewUrlBinding.root.gone
//                    }
//                    is Response.Success -> {
//                        val urlMetadata = loadResponse.value.toUrlMetadata(url)
//
//                        if (urlMetadata != null) {
//                            layoutMessageLinkPreviewUrlBinding.apply {
//                                textViewMessageLinkPreviewUrlTitle.text = urlMetadata.title
//                                textViewMessageLinkPreviewUrlDomain.text = urlMetadata.domain
//                                if (urlMetadata.description == null) {
//                                    textViewMessageLinkPreviewUrlDescription.gone
//                                } else {
//                                    textViewMessageLinkPreviewUrlDescription.visible
//                                    textViewMessageLinkPreviewUrlDescription.text = urlMetadata.description
//                                }
//
//                                if (urlMetadata.imageUrl == null) {
//                                    imageViewMessageLinkPreviewUrlMainImage.gone
//                                } else {
//                                    imageViewMessageLinkPreviewUrlMainImage.visible
//                                    imageLoader.load(
//                                        imageViewMessageLinkPreviewUrlMainImage,
//                                        urlMetadata.imageUrl
//                                    )
//                                }
//
//                                if (urlMetadata.favIconUrl == null) {
//                                    imageViewMessageLinkPreviewUrlFavicon.gone
//                                } else {
//                                    imageViewMessageLinkPreviewUrlFavicon.visible
//                                    imageLoader.load(
//                                        imageViewMessageLinkPreviewUrlFavicon,
//                                        urlMetadata.favIconUrl
//                                    )
//                                }
//                            }
//                            layoutMessageLinkPreviewUrlBinding.progressBarLinkPreview.gone
//                            layoutMessageLinkPreviewUrlBinding.constraintLayoutUrlLinkPreview.visible
//                        } else {
//                            layoutMessageLinkPreviewUrlBinding.root.gone
//                        }
//                    }
//                }
//            }
//        }
//    }
}
