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
import chat.sphinx.common_player.viewstate.RecommendationsPodcastPlayerViewState
import chat.sphinx.concept_connectivity_helper.ConnectivityHelper
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.wrapper_podcast.PodcastEpisode
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecommendedItemsAdapter (
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: CommonPlayerScreenViewModel,
    private val connectivityHelper: ConnectivityHelper,
): RecyclerView.Adapter<RecommendedItemsAdapter.RecommendedItemViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<PodcastEpisode>,
        private val newList: List<PodcastEpisode>,
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

                val same: Boolean = old.id == new.id

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

                val same: Boolean = old.playing == new.playing

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

    private val podcastEpisodes = ArrayList<PodcastEpisode>()

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->

                var episodes = ArrayList<PodcastEpisode>()

                if (viewState is RecommendationsPodcastPlayerViewState.PodcastViewState) {
                    episodes = viewState.podcast.getEpisodesListCopy()
                }

                if (episodes.isNotEmpty()) {
                    if (podcastEpisodes.isEmpty()) {
                        podcastEpisodes.addAll(episodes)
                        this@RecommendedItemsAdapter.notifyDataSetChanged()
                    } else {

                        val diff = Diff(podcastEpisodes, episodes)

                        withContext(viewModel.dispatchers.default) {
                            DiffUtil.calculateDiff(diff)
                        }.let { result ->

                            if (!diff.sameList) {
                                podcastEpisodes.clear()
                                podcastEpisodes.addAll(episodes)
                                result.dispatchUpdatesTo(this@RecommendedItemsAdapter)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return podcastEpisodes.size
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

    private fun getImageLoaderOptions(episode: PodcastEpisode): ImageLoaderOptions {
        if (episode.isMusicClip) {
            return imagePodcastLoaderOptions
        }
        if (episode.isYouTubeVideo) {
            return imageVideoLoaderOptions
        }
        return imagePodcastLoaderOptions
    }

    inner class RecommendedItemViewHolder(
        private val binding: LayoutRecommendedListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var episode: PodcastEpisode? = null

        init {
            binding.layoutConstraintRecommendedHolder.setOnClickListener {
                episode?.let { podcastEpisode ->
                    if (connectivityHelper.isNetworkConnected()) {
                        viewModel.playEpisodeFromList(podcastEpisode)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {

                val podcastEpisode: PodcastEpisode = podcastEpisodes.getOrNull(position) ?: let {
                    episode = null
                    return
                }
                episode = podcastEpisode
                disposable?.dispose()
                holderJob?.cancel()

                podcastEpisode.image?.value?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewRecommendedImage,
                            imageUrl,
                            getImageLoaderOptions(podcastEpisode)
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJob = job
                    }
                } ?: run {
                    imageViewRecommendedImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, podcastEpisode.getPlaceHolderImageRes())
                    )
                }

                textViewRecommendedTitle.text = podcastEpisode.description?.value ?: "-"
                textViewRecommendedDescription.text = podcastEpisode.title.value

                imageViewItemRowRecommendationType.setImageDrawable(
                    ContextCompat.getDrawable(root.context, podcastEpisode.getIconType())
                )

                root.setBackgroundColor(
                    root.context.getColor(
                        if (podcastEpisode.playing) R.color.semiTransparentPrimaryBlue else R.color.headerBG
                    )
                )
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}

inline fun PodcastEpisode.getPlaceHolderImageRes(): Int {
    if (isMusicClip) {
        return R.drawable.ic_podcast_placeholder
    }
    if (isYouTubeVideo) {
        return R.drawable.ic_video_placeholder
    }
    return R.drawable.ic_podcast_placeholder
}

inline fun PodcastEpisode.getIconType(): Int {
    if (isTwitterSpace) {
        return R.drawable.ic_twitter_space_type
    }
    if (isPodcast) {
        return R.drawable.ic_podcast_type
    }
    if (isYouTubeVideo) {
        return R.drawable.ic_youtube_type
    }
    return R.drawable.ic_podcast_type
}