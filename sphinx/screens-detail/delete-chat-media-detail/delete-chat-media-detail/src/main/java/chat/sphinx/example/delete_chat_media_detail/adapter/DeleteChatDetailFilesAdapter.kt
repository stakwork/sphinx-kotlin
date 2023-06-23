package chat.sphinx.example.delete_chat_media_detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.delete.chat.media.detail.R
import chat.sphinx.delete.chat.media.detail.databinding.ChatDeleteFileListItemHolderBinding
import chat.sphinx.example.delete_chat_media_detail.model.ChatFile
import chat.sphinx.example.delete_chat_media_detail.ui.DeleteChatMediaDetailViewModel
import chat.sphinx.example.delete_chat_media_detail.viewstate.DeleteChatMediaDetailViewState
import chat.sphinx.resources.getString
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeleteChatDetailFilesAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: DeleteChatMediaDetailViewModel,
    ): RecyclerView.Adapter<DeleteChatDetailFilesAdapter.ChatFileToDeleteViewHolder>(), DefaultLifecycleObserver {


    companion object {
        const val IMAGE = "image"
        const val VIDEO = "video"
        const val PDF = "pdf"
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

    private val chatFiles = ArrayList<ChatFile>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.viewStateContainer.collect { viewState ->

                var list: List<ChatFile> = if (viewState is DeleteChatMediaDetailViewState.FileList) {
                    viewState.files.filter { it.mediaType != IMAGE && it.mediaType != VIDEO }
                } else {
                    listOf()
                }

                if (chatFiles.isEmpty()) {
                    chatFiles.addAll(list)
                    this@DeleteChatDetailFilesAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(chatFiles, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            chatFiles.clear()
                            chatFiles.addAll(list)
                            result.dispatchUpdatesTo(this@DeleteChatDetailFilesAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return chatFiles.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeleteChatDetailFilesAdapter.ChatFileToDeleteViewHolder {
        val binding = ChatDeleteFileListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ChatFileToDeleteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeleteChatDetailFilesAdapter.ChatFileToDeleteViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ChatFileToDeleteViewHolder(
        private val binding: ChatDeleteFileListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var file: ChatFile? = null

        init {
            binding.root.setOnClickListener {
                file?.messageId?.let { messageId -> viewModel.changeItemSelection(messageId) }
            }
            setUpExtensionHolder()
        }

        private fun setUpExtensionHolder() {
            binding.constraintLayoutExtensionMark.apply {
            }
        }


        fun bind(position: Int) {
            binding.apply {
                val chatItem: ChatFile = chatFiles.getOrNull(position) ?: let {
                    file = null
                    return
                }
                file = chatItem

                textViewFileText.text = chatItem.fileName ?: chatItem.localFile?.name ?: "unnamed"
                textViewManageStorageFileSize.text = chatItem.size
                includeLayoutFileExtension.textViewExt.text = chatItem.ext ?: "DOC"

                if (chatItem.isSelected) {
                    imageViewCheckMark.visible
                    includeLayoutFileExtension.root.gone
                    viewStorageSeparatorDividerOne.setBackgroundColor(ContextCompat.getColor(root.context, R.color.blueDivider))
                    root.setBackgroundResource(R.drawable.background_dark_blue_selected_screen)

                } else
                {
                    imageViewCheckMark.gone
                    includeLayoutFileExtension.root.visible
                    viewStorageSeparatorDividerOne.setBackgroundColor(ContextCompat.getColor(root.context, R.color.darkDivider))
                    root.setBackgroundResource(R.drawable.background_detail_screen)
                }
                includeLayoutFileExtension.root.backgroundTintList = when (chatItem.ext) {
                    PDF -> ContextCompat.getColorStateList(root.context, R.color.pdfRed)
                    else -> ContextCompat.getColorStateList(root.context, R.color.primaryBlue)
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