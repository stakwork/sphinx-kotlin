package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
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
import chat.sphinx.dashboard.ui.feed.listen.FeedListenViewModel
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedListenNowAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: FeedListenViewModel,
): RecyclerView.Adapter<FeedListenNowAdapter.PodcastEpisodeViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<FeedItem>,
        private val newList: List<FeedItem>,
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
                    old.feed?.id                 == new.feed?.id


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
                    old.description             == new.description          &&
                    old.feed?.itemsCount        == new.feed?.itemsCount      &&
                    old.feed?.lastItem?.id      == new.feed?.lastItem?.id

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

    private val podcastEpisodes = ArrayList<FeedItem>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.feedsHolderViewStateFlow.collect { list ->

                val episodesList = mutableListOf<FeedItem>()

                list.forEach { feed ->
                    feed.lastItem?.let { feedItem ->
                        episodesList.add(feedItem)
                    }
                }

                if (podcastEpisodes.isEmpty()) {
                    podcastEpisodes.addAll(episodesList)
                    this@FeedListenNowAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(podcastEpisodes, episodesList)

                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            podcastEpisodes.clear()
                            podcastEpisodes.addAll(episodesList)
                            result.dispatchUpdatesTo(this@FeedListenNowAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return podcastEpisodes.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedListenNowAdapter.PodcastEpisodeViewHolder {
        val binding = LayoutFeedSquaredRowHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PodcastEpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedListenNowAdapter.PodcastEpisodeViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .build()
    }

    inner class PodcastEpisodeViewHolder(
        private val binding: LayoutFeedSquaredRowHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var episode: FeedItem? = null

        init {
            binding.layoutConstraintFeedHolder.setOnClickListener {
                episode?.let { nnEpisode ->
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.episodeItemSelected(nnEpisode)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val podcastEpisode: FeedItem = podcastEpisodes.getOrNull(position) ?: let {
                    episode = null
                    return
                }
                episode = podcastEpisode
                disposable?.dispose()
                holderJob?.cancel()

                podcastEpisode.imageUrlToShow?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
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
                } ?: run {
                    imageViewItemImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, R.drawable.ic_podcast_placeholder)
                    )
                }

                textViewItemName.text = podcastEpisode.titleToShow
                textViewItemDescription.text = podcastEpisode.descriptionToShow
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