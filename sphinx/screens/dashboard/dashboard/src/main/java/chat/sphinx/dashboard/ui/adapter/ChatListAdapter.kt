package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.ui.DashboardViewModel
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_chat.isMuted
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class ChatListAdapter(
    private val viewModel: DashboardViewModel,
    private val lifecycleOwner: LifecycleOwner,
): RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>(), DefaultLifecycleObserver {

    inner class ChatViewHolder(view: View): RecyclerView.ViewHolder(view)

    private inner class Diff(
        private val oldList: List<Chat>,
        private val newList: List<Chat>,
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                oldList[oldItemPosition].id == newList[newItemPosition].id
            } catch (e: IndexOutOfBoundsException) {
                false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                oldList[oldItemPosition].let { old ->
                    newList[newItemPosition].let { new ->
                        old.isMuted == new.isMuted          &&
                        old.seen == new.seen                &&
                        old.name == new.name                &&
                        old.photoUrl == new.photoUrl
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                false
            }
        }

    }

    private val chatsList = ArrayList<Chat>(viewModel.chatsStateFlow.value)
    private val supervisor = OnStartStopSupervisor(lifecycleOwner)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        supervisor.scope().launch {
            viewModel.chatsStateFlow.collect { newChats ->

                DiffUtil.calculateDiff(
                    Diff(chatsList, newChats)
                ).let { result ->
                    chatsList.clear()
                    chatsList.addAll(newChats)
                    result.dispatchUpdatesTo(this@ChatListAdapter)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return chatsList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListAdapter.ChatViewHolder {
        return ChatViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.layout_chat_holder, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ChatListAdapter.ChatViewHolder, position: Int) {
        chatsList.getOrNull(position)?.let { chat ->
            val layoutChatHolder: ConstraintLayout =
                holder.itemView.findViewById(R.id.layout_constraint_chat_holder)
            val imageViewChatHolder: ImageView =
                holder.itemView.findViewById(R.id.image_view_chat_holder)
            val textViewName: TextView =
                holder.itemView.findViewById(R.id.text_view_chat_holder_name)
            val imageViewLock: ImageView =
                holder.itemView.findViewById(R.id.image_view_chat_holder_lock)
            val textViewMessage: TextView =
                holder.itemView.findViewById(R.id.text_view_chat_holder_message)
            val imageViewNotification: ImageView =
                holder.itemView.findViewById(R.id.image_view_chat_holder_notification)
            val textViewTime: TextView =
                holder.itemView.findViewById(R.id.text_view_chat_holder_time)

            chat.name?.let {
                textViewName.text = it.value
            } // else contact name

            imageViewNotification.invisibleIfFalse(chat.isMuted())

            layoutChatHolder.setOnClickListener {
                @Exhaustive
                when (chat.type) {
                    is ChatType.Conversation -> {
                        supervisor.scope().launch {
                            viewModel.dashboardNavigator.toChatContact(chat.id)
                        }
                    }
                    is ChatType.Group -> {
                        supervisor.scope().launch {
                            viewModel.dashboardNavigator.toChatGroup(chat.id)
                        }
                    }
                    is ChatType.Tribe -> {
                        supervisor.scope().launch {
                            viewModel.dashboardNavigator.toChatTribe(chat.id)
                        }
                    }
                    is ChatType.Unknown -> {
                        // TODO: Warning message???
                    }
                }
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}