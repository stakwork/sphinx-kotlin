package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.LayoutChatHolderBinding
import chat.sphinx.dashboard.ui.DashboardViewModel
import chat.sphinx.dashboard.ui.collectChatViewState
import chat.sphinx.dashboard.ui.currentChatViewState
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_message.*
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

internal class ChatListAdapter(
    private val lifecycleOwner: LifecycleOwner,
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

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                val old = oldList[oldItemPosition]
                val new = newList[newItemPosition]

                when {
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
            } catch (e: IndexOutOfBoundsException) {
                false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                val old = oldList[oldItemPosition]
                val new = newList[newItemPosition]

                when {
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
            } catch (e: IndexOutOfBoundsException) {
                false
            }
        }

    }

    private val dashboardChats = ArrayList<DashboardChat>(viewModel.currentChatViewState.list)
    private val supervisor = OnStartStopSupervisor(lifecycleOwner)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        supervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
            viewModel.collectChatViewState { viewState ->

                if (dashboardChats.isEmpty()) {
                    dashboardChats.addAll(viewState.list)
                    this@ChatListAdapter.notifyDataSetChanged()
                } else {
                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(
                            Diff(dashboardChats, viewState.list)
                        )
                    }.let { result ->
                        dashboardChats.clear()
                        dashboardChats.addAll(viewState.list)
                        result.dispatchUpdatesTo(this@ChatListAdapter)
                    }
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return dashboardChats.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListAdapter.ChatViewHolder {
        val binding = LayoutChatHolderBinding.inflate(
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

    inner class ChatViewHolder(
        private val binding: LayoutChatHolderBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.apply {
                val dashboardChat = dashboardChats.getOrNull(position) ?: return

                // Set Defaults
                textViewChatHolderName.setTextColorExt(android.R.color.white)
                textViewChatHolderMessage.setTextColorExt(R.color.textHint)

                // Image
                // TODO: Setup COIL
                @Exhaustive
                when (dashboardChat) {
                    is DashboardChat.Active.Conversation -> {}
                    is DashboardChat.Active.GroupOrTribe -> {}
                    is DashboardChat.Inactive.Conversation -> {}
                }

                // Name
                textViewChatHolderName.text = if (dashboardChat.chatName != null) {
                    dashboardChat.chatName
                } else {
                    // Should never make it here, but just in case...
                    textViewChatHolderName.setTextColorExt(R.color.primaryRed)
                    "ERROR: NULL NAME"
                }

                // Lock
                imageViewChatHolderLock.invisibleIfFalse(dashboardChat is DashboardChat.Active)

                // Time
                textViewChatHolderTime.text = dashboardChat.getDisplayTime(today00)

                // Message
                val messageText = dashboardChat.getMessageText()

                if (messageText == DashboardChat.Active.DECRYPTION_ERROR) {
                    textViewChatHolderMessage.setTextColorExt(R.color.primaryRed)
                }

                textViewChatHolderMessage.text = messageText

                // Notification
                if (dashboardChat is DashboardChat.Active) {
                    imageViewChatHolderNotification.invisibleIfFalse(dashboardChat.chat.isMuted())
                } else {
                    imageViewChatHolderNotification.invisibleIfFalse(false)
                }

                // ClickListener
                layoutConstraintChatHolder.setOnClickListener {
                    @Exhaustive
                    when (dashboardChat) {
                        is DashboardChat.Active.Conversation -> {
                            // TODO: Use ContactId
                            supervisor.scope().launch {
                                viewModel.dashboardNavigator.toChatContact(dashboardChat.chat.id)
                            }
                        }
                        is DashboardChat.Active.GroupOrTribe -> {
                            if (dashboardChat.chat.type.isGroup()) {
                                supervisor.scope().launch {
                                    viewModel.dashboardNavigator.toChatGroup(dashboardChat.chat.id)
                                }
                            } else if (dashboardChat.chat.type.isTribe()) {
                                supervisor.scope().launch {
                                    viewModel.dashboardNavigator.toChatTribe(dashboardChat.chat.id)
                                }
                            }
                        }
                        is DashboardChat.Inactive.Conversation -> {
                            // TODO: Implement contactID usage with `dashboardNavigator.toChatContact`
                        }
                    }
                }
            }
        }

    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}