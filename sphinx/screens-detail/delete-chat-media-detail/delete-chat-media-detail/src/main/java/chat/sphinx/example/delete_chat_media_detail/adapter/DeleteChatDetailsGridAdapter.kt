package chat.sphinx.example.delete_chat_media_detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.delete.chat.media.detail.R
import chat.sphinx.delete.chat.media.detail.databinding.StorageGridImageListItemHolderBinding
import chat.sphinx.example.delete_chat_media_detail.model.ChatFile
import chat.sphinx.example.delete_chat_media_detail.ui.DeleteChatMediaDetailViewModel
import chat.sphinx.example.delete_chat_media_detail.viewstate.DeleteChatMediaDetailViewState
import chat.sphinx.wrapper_message_media.MediaType
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeleteChatDetailsGridAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: DeleteChatMediaDetailViewModel,
    private val userColorsHelper: UserColorsHelper,
    ): RecyclerView.Adapter<RecyclerView.ViewHolder>(), DefaultLifecycleObserver {

    companion object {
        const val VIEW_TYPE_IMAGE = 0
        const val VIEW_TYPE_AUDIO = 1
        const val VIEW_TYPE_VIDEO = 2
        const val VIEW_TYPE_ATTACHMENT = 3
    }

    private inner class Diff(
        private val oldList: List<ChatFile>,
        private val newList: List<ChatFile>,
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
                    old.messageId  == new.messageId && old.isSelected == new.isSelected

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
                    old.messageId  == new.messageId && old.isSelected == new.isSelected
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

    override fun getItemViewType(position: Int): Int {
        val chatFilesList = chatFiles.getOrNull(position)
        return when (chatFilesList?.mediaType) {
            MediaType.IMAGE -> VIEW_TYPE_IMAGE
            MediaType.AUDIO -> VIEW_TYPE_AUDIO
            MediaType.VIDEO -> VIEW_TYPE_VIDEO
            else -> VIEW_TYPE_ATTACHMENT
        }
    }

    private val chatFiles = ArrayList<ChatFile>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.viewStateContainer.collect { viewState ->

                var list: List<ChatFile> = if (viewState is DeleteChatMediaDetailViewState.FileList) {
                    viewState.files
                } else {
                    listOf()
                }

                if (chatFiles.isEmpty()) {
                    chatFiles.addAll(list)
                    this@DeleteChatDetailsGridAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(chatFiles, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            chatFiles.clear()
                            chatFiles.addAll(list)
                            result.dispatchUpdatesTo(this@DeleteChatDetailsGridAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return chatFiles.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = StorageGridImageListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return when (viewType) {
            VIEW_TYPE_IMAGE -> ImageViewHolder(binding)
                VIEW_TYPE_AUDIO -> AudioViewHolder(binding)
                VIEW_TYPE_VIDEO -> VideoViewHolder(binding)
            else -> AttachmentViewHolder(binding)
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            VIEW_TYPE_IMAGE == getItemViewType(position) -> {
                (holder as ImageViewHolder).bind(position)
            }
            VIEW_TYPE_AUDIO == getItemViewType(position) -> {
                (holder as AudioViewHolder).bind(position)
            }
            VIEW_TYPE_VIDEO == getItemViewType(position) -> {
                (holder as VideoViewHolder).bind(position)
            }
            VIEW_TYPE_ATTACHMENT == getItemViewType(position) -> {
                (holder as AttachmentViewHolder).bind(position)
            }
        }
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .build()
    }

    inner class ImageViewHolder(
        private val binding: StorageGridImageListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var file: ChatFile? = null

        init {
            binding.root.setOnClickListener {
                file?.messageId?.let { messageId -> viewModel.changeItemSelection(messageId) }
            }
            setUpPlaceHolder()
        }

        private fun setUpPlaceHolder() {
            binding.imageViewPlaceHolder.setImageDrawable(
                ContextCompat.getDrawable(
                    binding.root.context,
                    R.drawable.ic_chat_delete_image
                ))
        }

        fun bind(position: Int) {
            binding.apply {
                val chatItem: ChatFile = chatFiles.getOrNull(position) ?: let {
                    file = null
                    return
                }
                file = chatItem

                textViewFileSize.text = chatItem.size

                if (chatItem.isSelected) {
                    imageViewAlpha.visible
                    imageViewCheckMark.visible
                    textViewFileSize.gone
                } else
                {
                    imageViewAlpha.gone
                    imageViewCheckMark.gone
                    textViewFileSize.visible
                }
            }
        }

        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }
    }

    inner class AudioViewHolder(
        private val binding: StorageGridImageListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var file: ChatFile? = null


        init {
            binding.root.setOnClickListener {
                file?.messageId?.let { messageId -> viewModel.changeItemSelection(messageId) }
            }
            setUpPlaceHolder()
        }

        private fun setUpPlaceHolder() {
            binding.imageViewPlaceHolder.setImageDrawable(
                ContextCompat.getDrawable(
                    binding.root.context,
                    R.drawable.ic_chat_delete_audio
                ))
        }

        fun bind(position: Int) {
            binding.apply {
                val chatItem: ChatFile = chatFiles.getOrNull(position) ?: let {
                    file = null
                    return
                }
                file = chatItem

                textViewFileSize.text = chatItem.size

                if (chatItem.isSelected) {
                    imageViewAlpha.visible
                    imageViewCheckMark.visible
                    textViewFileSize.gone
                } else
                {
                    imageViewAlpha.gone
                    imageViewCheckMark.gone
                    textViewFileSize.visible
                }
            }
        }

        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }
    }

    inner class VideoViewHolder(
        private val binding: StorageGridImageListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var file: ChatFile? = null

        init {
            binding.root.setOnClickListener {
                file?.messageId?.let { messageId -> viewModel.changeItemSelection(messageId) }
            }
            setUpPlaceHolder()
        }

        private fun setUpPlaceHolder() {
            binding.imageViewPlaceHolder.setImageDrawable(
                ContextCompat.getDrawable(
                    binding.root.context,
                    R.drawable.ic_chat_delete_video
                ))
        }

        fun bind(position: Int) {
            binding.apply {
                val chatItem: ChatFile = chatFiles.getOrNull(position) ?: let {
                    file = null
                    return
                }
                file = chatItem

                textViewFileSize.text = chatItem.size

                if (chatItem.isSelected) {
                    imageViewAlpha.visible
                    imageViewCheckMark.visible
                    textViewFileSize.gone
                } else
                {
                    imageViewAlpha.gone
                    imageViewCheckMark.gone
                    textViewFileSize.visible
                }
            }
        }

        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }
    }

    inner class AttachmentViewHolder(
        private val binding: StorageGridImageListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var file: ChatFile? = null

        init {
            binding.root.setOnClickListener {
                file?.messageId?.let { messageId -> viewModel.changeItemSelection(messageId) }
            }
            setUpPlaceHolder()
        }

        private fun setUpPlaceHolder() {
            binding.imageViewPlaceHolder.setImageDrawable(
                ContextCompat.getDrawable(
                    binding.root.context,
                    R.drawable.ic_chat_delete_attachment
                ))
        }

        fun bind(position: Int) {
            binding.apply {
                val chatItem: ChatFile = chatFiles.getOrNull(position) ?: let {
                    file = null
                    return
                }
                file = chatItem

                textViewFileSize.text = chatItem.size

                if (chatItem.isSelected) {
                    imageViewAlpha.visible
                    imageViewCheckMark.visible
                    textViewFileSize.gone
                } else
                {
                    imageViewAlpha.gone
                    imageViewCheckMark.gone
                    textViewFileSize.visible
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