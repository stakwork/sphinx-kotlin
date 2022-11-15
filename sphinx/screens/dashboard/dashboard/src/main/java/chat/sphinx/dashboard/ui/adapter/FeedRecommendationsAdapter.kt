package chat.sphinx.dashboard.ui.adapter

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
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.LayoutFeedSquaredRowHolderBinding
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.feed.FeedRecommendationsViewModel
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_feed.Feed
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedRecommendationsAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: FeedRecommendationsViewModel,
    private val viewModelDispatcher: CoroutineDispatchers,
): RecyclerView.Adapter<FeedRecommendationsAdapter.RecommendationViewHolder>(), DefaultLifecycleObserver {

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

    private val recommendations = ArrayList<Feed>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModelDispatcher.mainImmediate) {
            viewModel.feedRecommendationsHolderViewStateFlow.collect { list ->

                if (recommendations.isEmpty()) {
                    recommendations.addAll(list)
                    this@FeedRecommendationsAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(recommendations, list)

                    withContext(viewModelDispatcher.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            recommendations.clear()
                            recommendations.addAll(list)
                            result.dispatchUpdatesTo(this@FeedRecommendationsAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return recommendations.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedRecommendationsAdapter.RecommendationViewHolder {
        val binding = LayoutFeedSquaredRowHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return RecommendationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedRecommendationsAdapter.RecommendationViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imagePodcastLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .build()
    }

    private val imageVideoLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_video_placeholder)
            .build()
    }

    private val imageNewsletterLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_newsletter_placeholder)
            .build()
    }

    private fun getImageLoaderOptions(feed: Feed): ImageLoaderOptions {
        when (feed.feedType) {
            is FeedType.Podcast -> {
                return imagePodcastLoaderOptions
            }
            is FeedType.Video -> {
                return imageVideoLoaderOptions
            }
            is FeedType.Newsletter -> {
                return imageNewsletterLoaderOptions
            }
            else -> {}
        }
        return imagePodcastLoaderOptions
    }

    inner class RecommendationViewHolder(
        private val binding: LayoutFeedSquaredRowHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var feed: Feed? = null

        init {
            binding.layoutConstraintFeedHolder.setOnClickListener {
                feed?.let { nnFeed ->
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.feedRecommendationSelected(nnFeed)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val f: Feed = recommendations.getOrNull(position) ?: let {
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
                            getImageLoaderOptions(f)
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJob = job
                    }
                } ?: run {
                    imageViewItemImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, f.getPlaceHolderImageRes())
                    )
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