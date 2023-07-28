package chat.sphinx.threads.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.chat_common.ui.viewstate.messageholder.ReplyUserHolder
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.resources.databinding.LayoutChatImageSmallInitialHolderBinding
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.threads.R
import chat.sphinx.threads.databinding.ThreadsListItemHolderBinding
import chat.sphinx.threads.model.ThreadItem
import chat.sphinx.threads.ui.ThreadsViewModel
import chat.sphinx.threads.viewstate.ThreadsViewState
import chat.sphinx.wrapper_common.util.getInitials
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ThreadsAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: ThreadsViewModel,
    private val userColorsHelper: UserColorsHelper,
    ): RecyclerView.Adapter<ThreadsAdapter.MediaSectionViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<ThreadItem>,
        private val newList: List<ThreadItem>,
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
                    old.uuid == new.uuid

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
                    old.aliasAndColorKey  == new.aliasAndColorKey

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

    private val threadsItems = ArrayList<ThreadItem>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.viewStateContainer.collect { viewState ->

                var list: List<ThreadItem> = if (viewState is ThreadsViewState.ThreadList) {
                    viewState.threads
                } else {
                    listOf()
                }

                if (threadsItems.isEmpty()) {
                    threadsItems.addAll(list)
                    this@ThreadsAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(threadsItems, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            threadsItems.clear()
                            threadsItems.addAll(list)
                            result.dispatchUpdatesTo(this@ThreadsAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return threadsItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadsAdapter.MediaSectionViewHolder {
        val binding = ThreadsListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MediaSectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThreadsAdapter.MediaSectionViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    inner class MediaSectionViewHolder(
        private val binding: ThreadsListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var item: ThreadItem? = null

        init {
            binding.root.setOnClickListener {
                    item?.let { threadItem ->
                        viewModel.navigateToThreadDetail(threadItem.uuid)
                    }
                }
            }

        fun bind(position: Int) {
            binding.apply {

                val threadItem: ThreadItem = threadsItems.getOrNull(position) ?: let {
                    item = null
                    return
                }
                item = threadItem

                // General Info
                textViewContactHeaderName.text = threadItem.aliasAndColorKey.first?.value
                textViewThreadDate.text = threadItem.date
                textViewThreadMessageContent.text = threadItem.message
                textViewRepliesQuantity.text = threadItem.repliesAmount
                textViewThreadTime.text = threadItem.lastReplyDate

//                 User Profile Picture
                layoutLayoutChatImageSmallInitialHolder.apply {
                    textViewInitialsName.visible
                    textViewInitialsName.text =
                        threadItem.aliasAndColorKey.first?.value ?: root.context.getString(R.string.unknown)
                            .getInitials()
                    imageViewChatPicture.gone

                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        textViewInitialsName.setBackgroundRandomColor(
                            R.drawable.chat_initials_circle,
                            Color.parseColor(
                                threadItem.aliasAndColorKey.second?.let {
                                    userColorsHelper.getHexCodeForKey(
                                        it,
                                        root.context.getRandomHexCode()
                                    )
                                }
                            )
                        )
                    }.let { job ->
                        holderJobs.add(job)

                        threadItem.photoUrl?.let { photoUrl ->
                            textViewInitialsName.gone
                            imageViewChatPicture.visible

                            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                imageLoader.load(
                                    imageViewChatPicture,
                                    photoUrl.value,
                                    imageLoaderOptions
                                ).also {
                                    disposables.add(it)
                                }
                            }
                        }
                    }
                }

                // Replies pictures
                val replyUsers = threadItem.usersReplies.orEmpty()

                includeLayoutMessageRepliesGroup.apply {
                    val replyImageHolders = listOf(
                        Pair(layoutConstraintReplyImageHolder1, includeReplyImageHolder1),
                        Pair(layoutConstraintReplyImageHolder2, includeReplyImageHolder2),
                        Pair(layoutConstraintReplyImageHolder3, includeReplyImageHolder3),
                        Pair(layoutConstraintReplyImageHolder4, includeReplyImageHolder4),
                        Pair(layoutConstraintReplyImageHolder5, includeReplyImageHolder5),
                        Pair(layoutConstraintReplyImageHolder6, includeReplyImageHolder6)
                    )

                    fun bindUserToImageHolder(
                        user: ReplyUserHolder,
                        includeReplyImageHolder: Pair<ConstraintLayout, LayoutChatImageSmallInitialHolderBinding>
                    ) {
                        includeReplyImageHolder.first.visible


                        includeReplyImageHolder.second.apply {
                            circularBorder.visible
                            textViewInitialsName.visible
                            textViewInitialsName.text =
                                user.alias?.value ?: root.context.getString(R.string.unknown)
                                    .getInitials()
                            imageViewChatPicture.gone

                            if (user.repliesCount != null) {
                                imageViewDefaultAlpha.visible
                                textViewRepliesNumber.visible
                                textViewRepliesNumber.text = user.repliesCount
                            }

                            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                textViewInitialsName.setBackgroundRandomColor(
                                    R.drawable.chat_initials_circle,
                                    Color.parseColor(
                                        userColorsHelper.getHexCodeForKey(
                                            user.colorKey,
                                            root.context.getRandomHexCode()
                                        )
                                    )
                                )
                            }.let { job ->
                                holderJobs.add(job)

                                user.photoUrl?.let { photoUrl ->
                                    textViewInitialsName.gone
                                    imageViewChatPicture.visible

                                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                        imageLoader.load(
                                            imageViewChatPicture,
                                            photoUrl.value,
                                            imageLoaderOptions
                                        ).also {
                                            disposables.add(it)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    for (i in replyUsers.indices) {
                        val user = replyUsers[i]
                        val userImageView = replyImageHolders[i]
                        bindUserToImageHolder(user, userImageView)
                    }
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