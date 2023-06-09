package chat.sphinx.delete_chat_media.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.delete.chat.media.R
import chat.sphinx.delete.chat.media.databinding.ChatDeleteListItemHolderBinding
import chat.sphinx.delete_chat_media.model.ChatToDelete
import chat.sphinx.delete_chat_media.ui.DeleteChatMediaViewModel
import chat.sphinx.delete_chat_media.viewstate.DeleteChatMediaViewState
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.resources.setBackgroundRandomColor
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeleteChatAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: DeleteChatMediaViewModel,
    private val userColorsHelper: UserColorsHelper,
    ): RecyclerView.Adapter<DeleteChatAdapter.ChatToDeleteViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<ChatToDelete>,
        private val newList: List<ChatToDelete>,
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

                val same: Boolean =
                    old.chatId == new.chatId

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

                val same: Boolean =
                    old.chatId  == new.chatId

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

    private val chatItems = ArrayList<ChatToDelete>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.viewStateContainer.collect { viewState ->

                var list: List<ChatToDelete> = if (viewState is DeleteChatMediaViewState.ChatList) {
                    viewState.chats
                } else {
                    listOf()
                }

                if (chatItems.isEmpty()) {
                    chatItems.addAll(list)
                    this@DeleteChatAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(chatItems, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            chatItems.clear()
                            chatItems.addAll(list)
                            result.dispatchUpdatesTo(this@DeleteChatAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return chatItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeleteChatAdapter.ChatToDeleteViewHolder {
        val binding = ChatDeleteListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ChatToDeleteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeleteChatAdapter.ChatToDeleteViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .build()
    }

    inner class ChatToDeleteViewHolder(
        private val binding: ChatDeleteListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var chat: ChatToDelete? = null

        init {
            binding.root.setOnClickListener {
                lifecycleOwner.lifecycleScope.launch {
                    chat?.chatId?.let { chatId ->
                        viewModel.navigator.toDeleteChatDetail(chatId)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val chatItem: ChatToDelete = chatItems.getOrNull(position) ?: let {
                    chat = null
                    return
                }
                chat = chatItem

                chatItem.photoUrl?.value?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            includeLayoutChatImageInitialHolder.imageViewChatPicture,
                            imageUrl,
                            imageLoaderOptions
                        ).also {
                            disposables.add(it)
                        }
                    }.let { job ->
                        holderJobs.add(job)
                    }
                } ?: run {
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        includeLayoutChatImageInitialHolder.apply {
                            imageViewChatPicture.gone
                            textViewInitials.apply {
                                visible
                                text = chatItem.initials.initials
                                setBackgroundRandomColor(
                                    R.drawable.chat_initials_circle,
                                    Color.parseColor(
                                        userColorsHelper.getHexCodeForKey(
                                            chatItem.initials.colorKey,
                                            root.context.getRandomHexCode(),
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
                textViewManageStorageElementText.text = chat?.contactAlias
                textViewManageStorageElementNumber.text = chat?.size

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