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
import chat.sphinx.dashboard.ui.feed.FeedDownloadedViewModel
import chat.sphinx.wrapper_common.timeAgo
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedDownloadedAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: FeedDownloadedViewModel,
    private val viewModelDispatcher: CoroutineDispatchers,
    ): RecyclerView.Adapter<FeedDownloadedAdapter.DownloadedPodcastEpisodeViewHolder>(), DefaultLifecycleObserver {

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
                    old.feed?.id                 == new.feed?.id    &&
                    old.id                       == new.id

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
                    old.title                             == new.title                              &&
                    old.description                       == new.description                        &&
                    old.feed?.itemsCount                  == new.feed?.itemsCount                   &&
                    old.feed?.lastItem?.id                == new.feed?.lastItem?.id                 &&
                    old.contentEpisodeStatus?.currentTime == new.contentEpisodeStatus?.currentTime  &&
                    old.contentEpisodeStatus?.duration    == new.contentEpisodeStatus?.duration

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

        onStopSupervisor.scope.launch(viewModelDispatcher.mainImmediate) {
            viewModel.feedDownloadedHolderViewStateFlow.collect { list ->

                val episodesList = mutableListOf<FeedItem>()

                list.forEach { feedItem ->
                        episodesList.add(feedItem)
                    }

                if (podcastEpisodes.isEmpty()) {
                    podcastEpisodes.addAll(episodesList)
                    this@FeedDownloadedAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(podcastEpisodes, episodesList)

                    withContext(viewModelDispatcher.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            podcastEpisodes.clear()
                            podcastEpisodes.addAll(episodesList)
                            result.dispatchUpdatesTo(this@FeedDownloadedAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return podcastEpisodes.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedDownloadedAdapter.DownloadedPodcastEpisodeViewHolder {
        val binding = LayoutFeedSquaredRowHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DownloadedPodcastEpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedDownloadedAdapter.DownloadedPodcastEpisodeViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .build()
    }

    inner class DownloadedPodcastEpisodeViewHolder(
        private val binding: LayoutFeedSquaredRowHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var episode: FeedItem? = null

        init {
            binding.layoutConstraintFeedHolder.setOnClickListener {
                episode?.let { nnEpisode ->
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.feedDownloadedSelected(nnEpisode)
                    }
                }
            }
            binding.seekBarCurrentTimeEpisodeProgress.setOnTouchListener { _, _ -> true }
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
                } ?: run {
                    imageViewItemImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, R.drawable.ic_podcast_placeholder)
                    )
                }

                textViewItemName.goneIfFalse(podcastEpisode.titleToShow.isNotEmpty())
                textViewItemDescription.goneIfFalse(podcastEpisode.descriptionToShow.isNotEmpty())

                textViewItemName.text = podcastEpisode.titleToShow
                textViewItemDescription.text = podcastEpisode.descriptionToShow

                textViewItemPublishTime.text = podcastEpisode.datePublished?.timeAgo()

                val currentTime = (podcastEpisode.contentEpisodeStatus?.currentTime?.value ?: 0).toInt()
                val duration = (podcastEpisode.contentEpisodeStatus?.duration?.value ?: 0).toInt()

                textViewItemEpisodeTime.goneIfFalse(currentTime > 0 || duration > 0)

                val progress = getSeekbarProgress(duration, currentTime)
                seekBarCurrentTimeEpisodeProgress.progress = progress
                seekBarCurrentTimeEpisodeProgress.goneIfFalse(currentTime > 0)

                if (currentTime > 0 && duration > 0) {
                    val timeLeft = duration - currentTime
                    textViewItemEpisodeTime.text = binding.root.context.getString(R.string.time_left, "${timeLeft.toHrAndMin()}")
                } else if (duration > 0) {
                    textViewItemEpisodeTime.text = "${duration.toHrAndMin()}"
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

    private fun getSeekbarProgress(duration: Int, currentTime: Int): Int {
        return try {
            currentTime * 100 / duration
        } catch (e: ArithmeticException) {
            0
        }
    }
}