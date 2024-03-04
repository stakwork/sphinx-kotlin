package chat.sphinx.dashboard.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.LayoutChatListChatHolderBinding
import chat.sphinx.dashboard.ui.ChatListViewModel
import chat.sphinx.dashboard.ui.collectChatViewState
import chat.sphinx.dashboard.ui.currentChatViewState
import chat.sphinx.resources.*
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.invite.*
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_message.*
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

internal class ChatListAdapter(
    private val recyclerView: RecyclerView,
    private val layoutManager: LinearLayoutManager,
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: ChatListViewModel,
    private val userColorsHelper: UserColorsHelper
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
                    old is DashboardChat.Inactive.Invite && new is DashboardChat.Inactive.Invite -> {
                        old.invite?.status == new.invite?.status &&
                        old.invite?.id              == new.invite?.id               &&
                        old.contact.status          == new.contact.status
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
                        old.chat.notify             == new.chat.notify              &&
                        old.chat.seen               == new.chat.seen                &&
                        old.chat.photoUrl           == new.chat.photoUrl
                    }
                    old is DashboardChat.Inactive.Invite && new is DashboardChat.Inactive.Invite -> {
                        old.invite?.status == new.invite?.status &&
                        old.invite?.id              == new.invite?.id               &&
                        old.contact.status          == new.contact.status
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
                            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                            dashboardChats.clear()
                            dashboardChats.addAll(viewState.list)
                            result.dispatchUpdatesTo(this@ChatListAdapter)

                            if (
                                firstVisibleItemPosition == 0                               &&
                                recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE
                            ) {
                                recyclerView.scrollToPosition(0)
                            }
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
        val binding = LayoutChatListChatHolderBinding.inflate(
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
            .build()
    }

    inner class ChatViewHolder(
        private val binding: LayoutChatListChatHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var disposable: Disposable? = null
        private var dChat: DashboardChat? = null
        private var badgeJob: Job? = null
        private var mentionsJob: Job? = null

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
                                if (dashboardChat.chat.type.isTribe()) {
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
                        is DashboardChat.Inactive.Invite -> {
                            dashboardChat.invite?.let { invite ->
                                lifecycleOwner.lifecycleScope.launch {
                                    viewModel.dashboardNavigator.toQRCodeDetail(
                                        invite.inviteString.value,
                                        binding.root.context.getString(
                                            R.string.invite_qr_code_header_name
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun bind(position: Int) {
            binding.apply {
                val dashboardChat: DashboardChat = dashboardChats.getOrNull(position) ?: let {
                    dChat = null
                    return
                }
                dChat = dashboardChat
                disposable?.dispose()
                badgeJob?.cancel()
                mentionsJob?.cancel()

                // Set Defaults
                layoutConstraintChatHolderBorder.goneIfFalse(position != dashboardChats.lastIndex)
                textViewDashboardChatHolderName.setTextColorExt(android.R.color.white)
                textViewChatHolderMessage.setTextColorExt(R.color.placeholderText)
                textViewChatHolderMessage.setTextFont(R.font.roboto_regular)
                textViewDashboardChatHolderBadgeCount.invisibleIfFalse(false)

                // Image
                dashboardChat.photoUrl.let { url ->

                    includeDashboardChatHolderInitial.apply {
                        imageViewChatPicture.goneIfFalse(url != null)
                        textViewInitials.goneIfFalse(url == null)
                    }

                    if (url != null) {
                        onStopSupervisor.scope.launch(viewModel.dispatchers.mainImmediate) {
                            imageLoader.load(
                                includeDashboardChatHolderInitial.imageViewChatPicture,
                                url.value,
                                imageLoaderOptions
                            )
                        }
                    } else {
                        includeDashboardChatHolderInitial.textViewInitials.text =
                            dashboardChat.chatName?.getInitials() ?: ""

                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                            includeDashboardChatHolderInitial.textViewInitials
                                .setInitialsColor(
                                    dashboardChat.getColorKey()?.let { colorKey ->
                                        Color.parseColor(
                                            userColorsHelper.getHexCodeForKey(
                                                colorKey,
                                                root.context.getRandomHexCode()
                                            )
                                        )
                                    },
                                    R.drawable.chat_initials_circle
                                )
                        }
                    }

                }

                // Name
                val chatName = if (dashboardChat is DashboardChat.Inactive.Invite) {
                    dashboardChat.getChatName(root.context)
                } else if (dashboardChat.chatName != null) {
                    dashboardChat.chatName
                } else {
                    // Should never make it here, but just in case...
                    textViewDashboardChatHolderName.setTextColorExt(R.color.primaryRed)
                    textViewChatHolderCenteredName.setTextColorExt(R.color.primaryRed)
                    root.context.getString(R.string.null_name_error)
                }

                textViewDashboardChatHolderName.text = chatName
                textViewChatHolderCenteredName.text = chatName


//                val chatHasMessages = (dashboardChat as? DashboardChat.Active)?.message != null
//                val activeChatOrInvite = ((dashboardChat is DashboardChat.Active && chatHasMessages) || dashboardChat is DashboardChat.Inactive.Invite)
//                layoutConstraintDashboardChatHolderMessage.invisibleIfFalse(activeChatOrInvite)
//                layoutConstraintDashboardChatNoMessageHolder.invisibleIfFalse(!activeChatOrInvite)

                if (dashboardChat is DashboardChat.Active.Conversation) {
                    imageViewChatHolderLock.text = getString(R.string.material_icon_name_lock)
                    progressBarChatStatus.gone
                    textViewChatStatus.gone
                }
                if (dashboardChat is DashboardChat.Inactive.Conversation) {
                    imageViewChatHolderLock.text = getString(R.string.material_icon_name_lock_open)
                    progressBarChatStatus.visible
                    textViewChatStatus.visible

                    textViewChatStatus.setTextColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.sphinxOrange
                        )
                    )
                }
                if (dashboardChat is DashboardChat.Active.GroupOrTribe) {
                    imageViewChatHolderLock.text = getString(R.string.material_icon_name_lock)
                    progressBarChatStatus.gone
                    textViewChatStatus.gone
                }

                // Time
                textViewChatHolderTime.text = dashboardChat.getDisplayTime(today00)

                // Message
                val messageText = dashboardChat.getMessageText(root.context)
                val hasUnseenMessages = dashboardChat.hasUnseenMessages()

                if (messageText == root.context.getString(R.string.decryption_error)) {
                    textViewChatHolderMessage.setTextColorExt(R.color.primaryRed)
                } else {
                    textViewChatHolderMessage.setTextColorExt(if (hasUnseenMessages) R.color.text else R.color.placeholderText)
                }

                textViewChatHolderMessage.setTextFont(if (hasUnseenMessages) R.font.roboto_bold else R.font.roboto_regular)

                textViewChatHolderMessage.text = messageText

                handleInviteLayouts()

                handleUnseenMessageCount()
                handleUnseenMentionsCount()

                // Notification
                if (dashboardChat is DashboardChat.Active) {
                    imageViewChatHolderNotification.invisibleIfFalse(dashboardChat.chat.isMuted())
                } else {
                    imageViewChatHolderNotification.invisibleIfFalse(false)
                }
            }
        }

        private fun handleInviteLayouts() {
            dChat?.let { nnDashboardChat ->
                binding.apply {
                    textViewChatHolderTime.visible
                    textViewChatHolderMessageIcon.gone
                    layoutConstraintDashboardChatHolderContact.visible
                    layoutConstraintDashboardChatHolderInvite.gone
                    layoutConstraintDashboardChatHolderInvitePrice.gone

                    if (nnDashboardChat is DashboardChat.Inactive.Invite) {
                        textViewChatHolderTime.gone
                        textViewChatHolderMessage.setTextFont(R.font.roboto_bold)
                        textViewChatHolderMessage.setTextColorExt(R.color.text)

                        layoutConstraintDashboardChatHolderContact.gone
                        layoutConstraintDashboardChatHolderInvite.visible

                        nnDashboardChat.getInviteIconAndColor()?.let { iconAndColor ->
                            textViewChatHolderMessageIcon.visible
                            textViewChatHolderMessageIcon.text = getString(iconAndColor.first)
                            textViewChatHolderMessageIcon.setTextColor(getColor(iconAndColor.second))
                        }

                        nnDashboardChat.getInvitePrice()?.let { price ->
                            val paymentPending = nnDashboardChat.invite?.status?.isPaymentPending() == true
                            layoutConstraintDashboardChatHolderInvitePrice.goneIfFalse(paymentPending)
                            textViewDashboardChatHolderInvitePrice.text = price.asFormattedString()
                        }
                    }
                }
            }
        }

        private fun handleUnseenMessageCount() {
            dChat?.let { nnDashboardChat ->
                badgeJob = onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    nnDashboardChat.unseenMessageFlow?.collect { unseen ->

                        binding.textViewDashboardChatHolderBadgeCount.apply {
                            if (unseen != null && unseen > 0) {
                                text = unseen.toString()
                            }

                            if (nnDashboardChat is DashboardChat.Active) {
                                val chatIsMutedOrOnlyMentions = (nnDashboardChat.chat.isMuted() || nnDashboardChat.chat.isOnlyMentions())

                                alpha = if (chatIsMutedOrOnlyMentions) 0.2f else 1.0f

                                backgroundTintList = binding.getColorStateList(if (chatIsMutedOrOnlyMentions) {
                                        R.color.washedOutReceivedText
                                    } else {
                                        R.color.primaryBlue
                                    }
                                )
                            }

                            goneIfFalse(nnDashboardChat.hasUnseenMessages())
                        }
                    }
                }
            }
        }

        private fun handleUnseenMentionsCount() {
            dChat?.let { nnDashboardChat ->
                mentionsJob = onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    nnDashboardChat.unseenMentionsFlow?.collect { unseenMentions ->

                        binding.textViewDashboardChatHolderMentionsCount.apply {
                            if (unseenMentions != null && unseenMentions > 0) {
                                text = "@ $unseenMentions"
                            }
                            goneIfFalse((unseenMentions ?: 0) > 0)
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

            mentionsJob?.let {
                if (!it.isActive) {
                    handleUnseenMentionsCount()
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
