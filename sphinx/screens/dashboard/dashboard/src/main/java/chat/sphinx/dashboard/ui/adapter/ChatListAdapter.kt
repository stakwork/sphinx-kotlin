package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.ui.DashboardViewModel
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_chat.isMuted
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ChatListAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: DashboardViewModel,
): RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>(), DefaultLifecycleObserver {

    inner class ChatViewHolder(view: View): RecyclerView.ViewHolder(view)

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
                oldList[oldItemPosition].let { old ->
                    newList[newItemPosition].let { new ->
                        old.chat.id == new.chat.id                              &&
                        old.chat.latestMessageId == new.chat.latestMessageId
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                oldList[oldItemPosition].let { old ->
                    newList[newItemPosition].let { new ->
                        old.chat.isMuted == new.chat.isMuted                    &&
                        old.chat.seen == new.chat.seen                          &&
                        old.chat.name == new.chat.name                          &&
                        old.chat.photoUrl == new.chat.photoUrl
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                false
            }
        }

    }

    private val dashboardChats = ArrayList<DashboardChat>(viewModel.chatsStateFlow.value)
    private val supervisor = OnStartStopSupervisor(lifecycleOwner)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        supervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
            viewModel.chatsStateFlow.collect { newDashboardChats ->
                if (dashboardChats.isEmpty() && newDashboardChats.isNotEmpty()) {
                    dashboardChats.addAll(newDashboardChats)
                    this@ChatListAdapter.notifyDataSetChanged()
                } else {
                    DiffUtil.calculateDiff(
                        Diff(dashboardChats, newDashboardChats)
                    ).let { result ->
                        dashboardChats.clear()
                        dashboardChats.addAll(newDashboardChats)
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
        return ChatViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.layout_chat_holder, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ChatListAdapter.ChatViewHolder, position: Int) {
        dashboardChats.getOrNull(position)?.let { dashboardChat ->
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

            @Exhaustive
            when (dashboardChat.chat.type) {
                is ChatType.Conversation -> {
                    // use contact's name
                    textViewName.text = "Contact"
                    textViewName.setTextColor(
                        ContextCompat.getColor(textViewName.context, android.R.color.white)
                    )
                }
                is ChatType.Group,
                is ChatType.Tribe -> {
                    val name = dashboardChat.chat.name

                    textViewName.text = if (name != null) {
                        textViewName.setTextColor(
                            ContextCompat.getColor(textViewName.context, android.R.color.white)
                        )
                        name.value
                    } else {
                        textViewName.setTextColor(
                            ContextCompat.getColor(textViewName.context, R.color.primaryRed)
                        )
                        "ERROR: NULL NAME"
                    }
                }
                is ChatType.Unknown -> {
                    textViewName.text = "ERROR: UNKNOWN CHAT TYPE"
                    textViewName.setTextColor(
                        ContextCompat.getColor(textViewName.context, R.color.primaryRed)
                    )
                }
            }

            textViewMessage.text = dashboardChat.chat.latestMessageId.toString()

            imageViewNotification.invisibleIfFalse(dashboardChat.chat.isMuted())

            layoutChatHolder.setOnClickListener {
                @Exhaustive
                when (dashboardChat.chat.type) {
                    is ChatType.Conversation -> {
                        supervisor.scope().launch {
                            viewModel.dashboardNavigator.toChatContact(dashboardChat.chat.id)
                        }
                    }
                    is ChatType.Group -> {
                        supervisor.scope().launch {
                            viewModel.dashboardNavigator.toChatGroup(dashboardChat.chat.id)
                        }
                    }
                    is ChatType.Tribe -> {
                        supervisor.scope().launch {
                            viewModel.dashboardNavigator.toChatTribe(dashboardChat.chat.id)
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