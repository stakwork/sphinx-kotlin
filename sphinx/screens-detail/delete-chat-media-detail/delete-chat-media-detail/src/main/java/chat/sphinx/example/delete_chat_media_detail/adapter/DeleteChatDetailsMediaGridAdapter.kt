package chat.sphinx.example.delete_chat_media_detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.delete.chat.media.detail.R
import chat.sphinx.delete.chat.media.detail.databinding.StorageGridImageListItemHolderBinding
import chat.sphinx.example.delete_chat_media_detail.model.ChatFile
import chat.sphinx.example.delete_chat_media_detail.ui.DeleteChatMediaDetailViewModel
import chat.sphinx.example.delete_chat_media_detail.uitl.VideoThumbnailDeleteUtil
import chat.sphinx.example.delete_chat_media_detail.viewstate.DeleteChatMediaDetailViewState
import chat.sphinx.wrapper_message_media.MediaType
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal class DeleteChatDetailsMediaGridAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: DeleteChatMediaDetailViewModel,
    ): RecyclerView.Adapter<RecyclerView.ViewHolder>(), DefaultLifecycleObserver {

    companion object {
        const val VIEW_TYPE_IMAGE = 0
        const val VIEW_TYPE_AUDIO = 1
        const val VIEW_TYPE_VIDEO = 2
        const val VIEW_TYPE_ATTACHMENT = 3

        const val IMAGE = "image"
        const val VIDEO = "video"
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

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .build()
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
                    viewState.files.filter { it.mediaType == IMAGE || it.mediaType == VIDEO }
                } else {
                    listOf()
                }

                if (chatFiles.isEmpty()) {
                    chatFiles.addAll(list)
                    this@DeleteChatDetailsMediaGridAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(chatFiles, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            chatFiles.clear()
                            chatFiles.addAll(list)
                            result.dispatchUpdatesTo(this@DeleteChatDetailsMediaGridAdapter)
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
                VIEW_TYPE_VIDEO -> VideoViewHolder(binding)
            else -> ImageViewHolder(binding)
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            VIEW_TYPE_IMAGE == getItemViewType(position) -> {
                (holder as ImageViewHolder).bind(position)
            }
            VIEW_TYPE_VIDEO == getItemViewType(position) -> {
                (holder as VideoViewHolder).bind(position)
            }
        }
    }

    inner class ImageViewHolder(
        private val binding: StorageGridImageListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var file: ChatFile? = null

        init {
            binding.root.setOnClickListener {
                file?.messageId?.let { messageId -> viewModel.changeItemSelection(messageId) }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val chatItem: ChatFile = chatFiles.getOrNull(position) ?: let {
                    file = null
                    return
                }
                file = chatItem

                textViewFileSize.text = chatItem.size
                imageViewPlaceHolder.gone
                imageViewFile.visible

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
                chatItem.localFile?.let { file ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewFile,
                            file,
                            imageLoaderOptions
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJob = job
                    }
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
        private fun setUpImageFromFile(videoAttachment: File){
            val thumbnail = VideoThumbnailDeleteUtil.loadThumbnail(videoAttachment)
            if (thumbnail != null) {
                binding.imageViewFile.visible
                binding.imageViewFile.setImageBitmap(thumbnail)
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val chatItem: ChatFile = chatFiles.getOrNull(position) ?: let {
                    file = null
                    return
                }
                file = chatItem

                textViewFileSize.text = chatItem.size
                file?.localFile?.let {
                    setUpImageFromFile(it)
                }

                if (chatItem.isSelected) {
                    imageViewAlpha.visible
                    imageViewCheckMark.visible
                    textViewFileSize.gone
                    imageViewPlaceHolder.gone
                } else
                {
                    imageViewAlpha.gone
                    imageViewCheckMark.gone
                    textViewFileSize.visible
                    imageViewPlaceHolder.visible
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