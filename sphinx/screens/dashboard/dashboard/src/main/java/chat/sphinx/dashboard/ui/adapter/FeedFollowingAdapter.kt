package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.LayoutFeedSquaredRowHolderBinding
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.feed.listen.FeedListenViewModel
import chat.sphinx.dashboard.ui.placeholder.PlaceholderContent.PlaceholderItem
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFollowingAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: FeedFollowingViewModel,
    private val viewModelDispatcher: CoroutineDispatchers,
): RecyclerView.Adapter<FeedFollowingAdapter.FeedViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<Feed>,
        private val newList: List<Feed>,
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
                    old.id                 == new.id


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
                    old.title                   == new.title                &&
                    old.itemsCount              == new.itemsCount      &&
                    old.lastItem?.id            == new.lastItem?.id

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

    private val feeds = ArrayList<Feed>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModelDispatcher.mainImmediate) {
            viewModel.feedsHolderViewStateFlow.collect { list ->

                if (feeds.isEmpty()) {
                    feeds.addAll(list)
                    this@FeedFollowingAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(feeds, list)

                    withContext(viewModelDispatcher.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            feeds.clear()
                            feeds.addAll(list)
                            result.dispatchUpdatesTo(this@FeedFollowingAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return feeds.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedFollowingAdapter.FeedViewHolder {
        val binding = LayoutFeedSquaredRowHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedFollowingAdapter.FeedViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .build()
    }

    inner class FeedViewHolder(
        private val binding: LayoutFeedSquaredRowHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var feed: Feed? = null

        init {
            binding.layoutConstraintFeedHolder.setOnClickListener {
                feed?.let { nnFeed ->
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.feedSelected(nnFeed)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val f: Feed = feeds.getOrNull(position) ?: let {
                    feed = null
                    return
                }
                feed = f
                disposable?.dispose()
                holderJob?.cancel()

                f.imageUrlToShow?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModelDispatcher.mainImmediate) {
                        imageLoader.load(
                            imageViewItemImage,
                            imageUrl.value,
                            imageLoaderOptions
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJob = job
                    }
                }

                textViewItemName.text = f.titleToShow
                textViewItemDescription.text = f.descriptionToShow
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