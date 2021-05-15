package chat.sphinx.chat_common.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.chat_common.ui.viewstate.messageholder.MessageHolderViewState
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.wrapper_view.Px
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageListAdapter(
    private val recyclerView: RecyclerView,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: ChatViewModel,
    private val imageLoader: ImageLoader<ImageView>,
): RecyclerView.Adapter<MessageListAdapter.MessageViewHolder>(), DefaultLifecycleObserver {

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
                    old is MessageHolderViewState.InComing && new is MessageHolderViewState.InComing -> {
                        old.background == new.background
                    }
                    old is MessageHolderViewState.OutGoing && new is MessageHolderViewState.OutGoing -> {
                        old.background == new.background
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
                        messages.clear()
                        messages.addAll(list)
                        result.dispatchUpdatesTo(this@MessageListAdapter)
                    }
                }
            }
        }
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

    inner class MessageViewHolder(
        private val binding: LayoutMessageHolderBinding
    ): RecyclerView.ViewHolder(binding.root) {

        private var disposable: Disposable? = null

        fun bind(position: Int) {
            binding.apply {
                val viewState = messages.elementAtOrNull(position) ?: return
                disposable?.dispose()

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    disposable = viewState.initialHolder.setInitialHolder(
                        binding.includeMessageHolderChatImageInitialHolder.textViewInitials,
                        binding.includeMessageHolderChatImageInitialHolder.imageViewChatPicture,
                        imageLoader
                    )
                }

                // TODO: refactor into view state
                viewState.message.messageContentDecrypted?.value?.let { content ->
                    includeMessageHolderMessageTypes.includeMessageTypeMessage.root.goneIfFalse(true)
                    includeMessageHolderMessageTypes.includeMessageTypeMessage.textViewMessageTypeMessage.text =
                        content
                } ?: includeMessageHolderMessageTypes.includeMessageTypeMessage.root.goneIfFalse(false)

                viewState.background.setBackground(recyclerViewWidth, binding)
            }
        }

    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}
