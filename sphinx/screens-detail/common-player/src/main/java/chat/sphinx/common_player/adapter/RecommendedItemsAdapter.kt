package chat.sphinx.common_player.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.common_player.R
import chat.sphinx.common_player.databinding.LayoutRecommendedListItemHolderBinding
import chat.sphinx.common_player.ui.CommonPlayerScreenViewModel
import chat.sphinx.common_player.viewstate.CommonPlayerScreenViewState
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.wrapper_feed.FeedRecommendation
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.annotation.meta.Exhaustive

class RecommendedItemsAdapter (
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: CommonPlayerScreenViewModel,
): RecyclerView.Adapter<RecommendedItemsAdapter.RecommendedItemViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<FeedRecommendation>,
        private val newList: List<FeedRecommendation>,
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
                    old.title                == new.title                &&
                    old.description          == new.description          &&
                    old.link                 == new.link

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

    private val recommendations = ArrayList<FeedRecommendation>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->
                @Exhaustive
                when(viewState) {
                    is CommonPlayerScreenViewState.Idle -> {
                        recommendations.removeAll(listOf())
                        this@RecommendedItemsAdapter.notifyDataSetChanged()
                    }
                    is CommonPlayerScreenViewState.FeedRecommendations -> {
                        if (recommendations.isEmpty()) {
                            recommendations.addAll(viewState.recommendations)
                            this@RecommendedItemsAdapter.notifyDataSetChanged()
                        } else {

                            val diff = Diff(recommendations, viewState.recommendations)

                            withContext(viewModel.default) {
                                DiffUtil.calculateDiff(diff)
                            }.let { result ->

                                if (!diff.sameList) {
                                    recommendations.clear()
                                    recommendations.addAll(viewState.recommendations)
                                    result.dispatchUpdatesTo(this@RecommendedItemsAdapter)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return recommendations.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendedItemViewHolder {
        val binding = LayoutRecommendedListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return RecommendedItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecommendedItemViewHolder, position: Int) {
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

    private fun getImageLoaderOptions(feed: FeedRecommendation): ImageLoaderOptions {
        when (feed.feedType) {
            "podcast" -> {
                return imagePodcastLoaderOptions
            }
            "youtube" -> {
                return imageVideoLoaderOptions
            }
            "newsletter" -> {
                return imageNewsletterLoaderOptions
            }
            else -> {}
        }
        return imagePodcastLoaderOptions
    }

    inner class RecommendedItemViewHolder(
        private val binding: LayoutRecommendedListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var feedRecommendation: FeedRecommendation? = null

        init {
//            binding.layoutConstraintRecommendedHolder.setOnClickListener {
//                feedRecommendation?.let { nnFeedRecommendation->
//                    lifecycleOwner.lifecycleScope.launch {
//
//                    }
//                }
//            }
        }

        fun bind(position: Int) {
            binding.apply {
                val f: FeedRecommendation = recommendations.getOrNull(position) ?: let {
                    feedRecommendation = null
                    return
                }
                feedRecommendation = f
                disposable?.dispose()
                holderJob?.cancel()

                if (f.imageUrl.isNotEmpty()) {
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewRecommendedImage,
                            f.imageUrl,
                            getImageLoaderOptions(f)
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJob = job
                    }
                } else {
                    imageViewRecommendedImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, f.getPlaceHolderImageRes())
                    )
                }

                textViewRecommendedTitle.text = f.title
                textViewRecommendedDescription.text = f.description
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}

inline fun FeedRecommendation.getPlaceHolderImageRes(): Int =
    when (feedType) {
        "podcast" -> {
            R.drawable.ic_podcast_placeholder
        }
        "youtube" -> {
            R.drawable.ic_video_placeholder
        }
        "newsletter" -> {
            R.drawable.ic_newsletter_placeholder
        }
        else -> {
            R.drawable.ic_podcast_placeholder
        }
    }