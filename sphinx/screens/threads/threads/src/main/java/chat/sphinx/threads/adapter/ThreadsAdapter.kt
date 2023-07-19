package chat.sphinx.threads.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.threads.R
import chat.sphinx.threads.databinding.ThreadsListItemHolderBinding
import chat.sphinx.threads.model.ThreadItem
import chat.sphinx.threads.ui.ThreadsViewModel
import chat.sphinx.threads.viewstate.ThreadsViewState
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
                    old.userName == new.userName

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
                    old.userName  == new.userName

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

    private val sectionItems = ArrayList<ThreadItem>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.viewStateContainer.collect { viewState ->

                var list: List<ThreadItem> = if (viewState is ThreadsViewState.Idle) {
                    listOf()
                } else {
                    listOf()
                }

                if (sectionItems.isEmpty()) {
                    sectionItems.addAll(list)
                    this@ThreadsAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(sectionItems, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            sectionItems.clear()
                            sectionItems.addAll(list)
                            result.dispatchUpdatesTo(this@ThreadsAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return sectionItems.size
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
            .placeholderResId(R.drawable.ic_podcast_type)
            .build()
    }

    inner class MediaSectionViewHolder(
        private val binding: ThreadsListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var section: ThreadItem? = null

        init {
//            binding.root.setOnClickListener {
//                lifecycleOwner.lifecycleScope.launch {
//                    section?.let { section ->
//                        viewModel.navigator.toDeleteMediaDetail(section.feedId)
//                    }
//                }
//            }
        }

        fun bind(position: Int) {
            binding.apply {
                val sectionItem: ThreadItem = sectionItems.getOrNull(position) ?: let {
                    section = null
                    return
                }
                section = sectionItem

//                sectionItem.image.let { imageUrl ->
//                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//                        imageLoader.load(
//                            imageViewElementPicture,
//                            imageUrl,
//                            imageLoaderOptions
//                        ).also {
//                            disposables.add(it)
//                        }
//                    }.let { job ->
//                        holderJobs.add(job)
//                    }
//                } ?: run {
//                    imageViewElementPicture.setImageDrawable(
//                        ContextCompat.getDrawable(root.context, R.drawable.ic_tribe)
//                    )
//                }
//                textViewManageStorageElementText.text = section?.title
//                textViewManageStorageElementNumber.text = section?.size

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