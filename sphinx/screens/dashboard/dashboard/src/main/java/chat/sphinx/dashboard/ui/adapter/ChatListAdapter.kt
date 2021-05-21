package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.LayoutDashboardChatHolderBinding
import chat.sphinx.dashboard.ui.DashboardViewModel
import chat.sphinx.dashboard.ui.collectChatViewState
import chat.sphinx.dashboard.ui.currentChatViewState
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.resources.setTextFont
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_message.*
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList


internal class ChatListAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: DashboardViewModel,
): RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<DashboardChat>,
        private val newList: List<DashboardChat>,
    ): DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        @Volatile
        var sameList: Boolean = oldListSize == newListSize

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                val old = oldList[oldItemPosition]
                val new = newList[newItemPosition]

                val same: Boolean = when {
                    old is DashboardChat.Active && new is DashboardChat.Active -> {
                        old.chat.id                 == new.chat.id                  &&
                        old.chat.latestMessageId    == new.chat.latestMessageId
                    }
                    old is DashboardChat.Inactive && new is DashboardChat.Inactive -> {
                        old.chatName                == new.chatName
                    }
                    else -> {
                        false
                    }
                }

                if (sameList) {
                    sameList = same
                }

                same
            } catch (e: IndexOutOfBoundsException) {
                sameList = false
                false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                val old = oldList[oldItemPosition]
                val new = newList[newItemPosition]

                val same: Boolean = when {
                    old is DashboardChat.Active && new is DashboardChat.Active -> {
                        old.chat.type               == new.chat.type                &&
                        old.chatName                == new.chatName                 &&
                        old.chat.isMuted            == new.chat.isMuted             &&
                        old.chat.seen               == new.chat.seen                &&
                        old.chat.photoUrl           == new.chat.photoUrl
                    }
                    old is DashboardChat.Inactive && new is DashboardChat.Inactive -> {
                        old.chatName == new.chatName
                    }
                    else -> {
                        false
                    }
                }

                if (sameList) {
                    sameList = same
                }

                same
            } catch (e: IndexOutOfBoundsException) {
                sameList = false
                false
            }
        }

    }

    private val dashboardChats = ArrayList<DashboardChat>(viewModel.currentChatViewState.list)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectChatViewState { viewState ->

                if (dashboardChats.isEmpty()) {
                    dashboardChats.addAll(viewState.list)
                    this@ChatListAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(dashboardChats, viewState.list)

                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            dashboardChats.clear()
                            dashboardChats.addAll(viewState.list)
                            result.dispatchUpdatesTo(this@ChatListAdapter)
                        }

                    }
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return dashboardChats.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListAdapter.ChatViewHolder {
        val binding = LayoutDashboardChatHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatListAdapter.ChatViewHolder, position: Int) {
        holder.bind(position)
    }

    private val today00: DateTime by lazy {
        DateTime.getToday00()
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    inner class ChatViewHolder(
        private val binding: LayoutDashboardChatHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var disposable: Disposable? = null
        private var dChat: DashboardChat? = null
        private var badgeJob: Job? = null

        init {
            binding.layoutConstraintChatHolder.setOnClickListener {
                dChat?.let { dashboardChat ->
                    @Exhaustive
                    when (dashboardChat) {
                        is DashboardChat.Active.Conversation -> {
                            lifecycleOwner.lifecycleScope.launch {
                                viewModel.dashboardNavigator.toChatContact(
                                    dashboardChat.chat.id,
                                    dashboardChat.contact.id
                                )
                            }
                        }
                        is DashboardChat.Active.GroupOrTribe -> {
                            lifecycleOwner.lifecycleScope.launch {
                                if (dashboardChat.chat.type.isGroup()) {
                                    viewModel.dashboardNavigator.toChatGroup(dashboardChat.chat.id)
                                } else if (dashboardChat.chat.type.isTribe()) {
                                    viewModel.dashboardNavigator.toChatTribe(dashboardChat.chat.id)
                                }

                            }
                        }
                        is DashboardChat.Inactive.Conversation -> {
                            lifecycleOwner.lifecycleScope.launch {
                                viewModel.dashboardNavigator.toChatContact(
                                    null,
                                    dashboardChat.contact.id
                                )
                            }
                        }
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val dashboardChat: DashboardChat = dashboardChats.getOrNull(position) ?: let {
                    dChat = null
                    return
                }
                dChat = dashboardChat
                disposable?.dispose()
                badgeJob?.cancel()

                // Set Defaults
                layoutConstraintChatHolderBorder.goneIfFalse(position != dashboardChats.lastIndex)
                textViewChatHolderName.setTextColorExt(android.R.color.white)
                textViewChatHolderMessage.setTextColorExt(R.color.placeholderText)
                textViewChatHolderMessage.setTextFont(R.font.roboto_regular)
                textViewBadgeCount.invisibleIfFalse(false)

                // Image
                dashboardChat.photoUrl.let { url ->

                    layoutDashboardChatInitialHolder.imageViewChatPicture.goneIfFalse(url != null)
                    layoutDashboardChatInitialHolder.textViewInitials.goneIfFalse(url == null)

                    if (url != null) {
                        onStopSupervisor.scope.launch(viewModel.dispatchers.mainImmediate) {
                            imageLoader.load(
                                layoutDashboardChatInitialHolder.imageViewChatPicture,
                                url.value,
                                imageLoaderOptions
                            )
                        }
                    } else {
                        layoutDashboardChatInitialHolder.textViewInitials.text =
                            dashboardChat.chatName?.getInitials() ?: ""
                        layoutDashboardChatInitialHolder.textViewInitials
                            .setBackgroundRandomColor(R.drawable.chat_initials_circle)
                    }

                }

                // Name
                val chatName = if (dashboardChat.chatName != null) {
                    dashboardChat.chatName
                } else {
                    // Should never make it here, but just in case...
                    textViewChatHolderName.setTextColorExt(R.color.primaryRed)
                    textViewChatHolderCenteredName.setTextColorExt(R.color.primaryRed)
                    "ERROR: NULL NAME"
                }

                textViewChatHolderName.text = chatName
                textViewChatHolderCenteredName.text = chatName

                // Lock
                val encryptedChat = dashboardChat.isEncrypted()
                imageViewChatHolderLock.invisibleIfFalse(encryptedChat)
                imageViewChatHolderCenteredLock.invisibleIfFalse(encryptedChat)

                layoutConstraintDashboardChatMessageHolder.invisibleIfFalse(dashboardChat is DashboardChat.Active)
                layoutConstraintDashboardChatNoMessageHolder.invisibleIfFalse(dashboardChat !is DashboardChat.Active)

                // Time
                textViewChatHolderTime.text = dashboardChat.getDisplayTime(today00)

                // Message
                val messageText = dashboardChat.getMessageText()
                val hastUnseenMessages = dashboardChat.hasUnseenMessages()

                if (messageText == DashboardChat.Active.DECRYPTION_ERROR) {
                    textViewChatHolderMessage.setTextColorExt(R.color.primaryRed)
                } else {
                    textViewChatHolderMessage.setTextColorExt(if (hastUnseenMessages) R.color.text else R.color.placeholderText)
                }

                textViewChatHolderMessage.setTextFont(if (hastUnseenMessages) R.font.roboto_bold else R.font.roboto_regular)

                textViewChatHolderMessage.text = messageText

                handleUnseenMessageCount()

                // Notification
                if (dashboardChat is DashboardChat.Active) {
                    imageViewChatHolderNotification.invisibleIfFalse(dashboardChat.chat.isMuted())
                } else {
                    imageViewChatHolderNotification.invisibleIfFalse(false)
                }
            }
        }

        private fun handleUnseenMessageCount() {
            dChat?.let { nnDashboardChat ->
                badgeJob = onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    nnDashboardChat.unseenMessageFlow?.collect { unseen ->
                        binding.textViewBadgeCount.apply {
                            if (unseen != null && unseen > 0) {
                                text = unseen.toString()
                            }
                            invisibleIfFalse(nnDashboardChat.hasUnseenMessages())
                        }
                    }
                }
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            badgeJob?.let {
                if (!it.isActive) {
                    handleUnseenMessageCount()
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
