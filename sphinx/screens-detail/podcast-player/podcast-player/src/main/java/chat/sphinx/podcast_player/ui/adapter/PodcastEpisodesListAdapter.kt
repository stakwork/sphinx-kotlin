package chat.sphinx.podcast_player.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_connectivity_helper.ConnectivityHelper
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.podcast_player.R
import chat.sphinx.podcast_player.ui.PodcastPlayerViewModel
import chat.sphinx.podcast_player.ui.viewstates.PodcastPlayerViewState
import chat.sphinx.resources.databinding.LayoutEpisodeGenericListItemHolderBinding
import chat.sphinx.resources.getString
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_podcast.PodcastEpisode
import io.matthewnelson.android_feature_screens.util.*
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList


internal class PodcastEpisodesListAdapter(
    private val recyclerView: RecyclerView,
    private val layoutManager: LinearLayoutManager,
    private val imageLoader: ImageLoader<ImageView>,
    private val connectivityHelper: ConnectivityHelper,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: PodcastPlayerViewModel,
): RecyclerView.Adapter<PodcastEpisodesListAdapter.EpisodeViewHolder>(), DefaultLifecycleObserver {

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

                val same: Boolean = old.playing == new.playing &&
                                    old.downloaded == new.downloaded &&
                                    old.played == new.played &&
                                    old.durationMilliseconds == new.durationMilliseconds &&
                                    old.currentTimeSeconds == new.currentTimeSeconds &&
                                    viewModel.isFeedItemDownloadInProgress(old.id) == viewModel.isFeedItemDownloadInProgress(new.id) &&
                                    viewModel.isEpisodeSoundPlaying(old) == viewModel.isEpisodeSoundPlaying(new)

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

                if (viewState is PodcastPlayerViewState.PodcastLoaded) {
                    episodes = if (viewState.downloadedOnly) {
                        viewState.podcast.getDownloadedEpisodesListCopy()
                    } else {
                        viewState.podcast.getEpisodesListCopy()
                    }
                }

                if (viewState is PodcastPlayerViewState.EpisodePlayed) {
                    episodes = if (viewState.downloadedOnly) {
                        viewState.podcast.getDownloadedEpisodesListCopy()
                    } else {
                        viewState.podcast.getEpisodesListCopy()
                    }
                }

                if (viewState is PodcastPlayerViewState.MediaStateUpdate) {
                    if (
                        viewState.state is MediaPlayerServiceState.ServiceActive.MediaState.Paused ||
                        viewState.state is MediaPlayerServiceState.ServiceActive.MediaState.Ended ||
                        viewState.state is MediaPlayerServiceState.ServiceActive.MediaState.Playing
                    ) {
                        episodes = if (viewState.downloadedOnly) {
                            viewState.podcast.getDownloadedEpisodesListCopy()
                        } else {
                            viewState.podcast.getEpisodesListCopy()
                        }
                    }
                }

                if (episodes.isNotEmpty()) {
                    if (podcastEpisodes.isEmpty()) {
                        podcastEpisodes.addAll(episodes)
                        this@PodcastEpisodesListAdapter.notifyDataSetChanged()
                    } else {

                        val diff = Diff(podcastEpisodes, episodes)

                        withContext(viewModel.dispatchers.default) {
                            DiffUtil.calculateDiff(diff)
                        }.let { result ->

                            if (!diff.sameList) {
                                podcastEpisodes.clear()
                                podcastEpisodes.addAll(episodes)
                                result.dispatchUpdatesTo(this@PodcastEpisodesListAdapter)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastEpisodesListAdapter.EpisodeViewHolder {
        val binding = LayoutEpisodeGenericListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PodcastEpisodesListAdapter.EpisodeViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .build()
    }

    inner class EpisodeViewHolder(
        private val binding: LayoutEpisodeGenericListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var disposable: Disposable? = null
        private var episode: PodcastEpisode? = null

        init {
            binding.buttonPlayEpisode.setOnClickListener {
                playEpisodeFromList()
            }
            binding.layoutConstraintEpisodeInfoContainer.setOnClickListener {
                episode?.let { nnEpisode ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.toEpisodeDescriptionScreen(nnEpisode.id)
                    }
                }
            }

            binding.buttonDownloadArrow.setOnClickListener {
                episode?.let { nnEpisode ->
                    viewModel.downloadMedia(nnEpisode) { downloadedFile ->
                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                            nnEpisode.localFile = downloadedFile
                            notifyItemChanged(position)
                        }
                    }
                    notifyItemChanged(position)
                }
            }

            binding.buttonAdditionalOptions.setOnClickListener {
                episode?.let { nnEpisode ->
                    viewModel.showOptionsFor(nnEpisode)
                }
            }

            binding.buttonEpisodeShare.setOnClickListener {
                episode?.id?.let { feedId ->
                    viewModel.share(feedId, binding.root.context)
                }
            }
        }

        private fun playEpisodeFromList(){
            episode?.let { podcastEpisode ->
                if (connectivityHelper.isNetworkConnected() || podcastEpisode.downloaded) {
                    viewModel.playEpisodeFromList(podcastEpisode)
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

                // General info
                textViewEpisodeHeader.text = podcastEpisode.titleToShow
                textViewEpisodeDescription.text = podcastEpisode.descriptionToShow
                textViewEpisodeDate.text = podcastEpisode.dateString

                // Set Duration Time
                val currentTime = (podcastEpisode.contentEpisodeStatus?.currentTime?.value ?: 0).toInt()
                val duration = (podcastEpisode.contentEpisodeStatus?.duration?.value ?: 0).toInt()

                textViewItemEpisodeTime.goneIfFalse(currentTime > 0 || duration > 0)
                circleSplit.goneIfFalse(currentTime > 0 || duration > 0)

                val progress = getSeekbarProgress(duration, currentTime)
                seekBarCurrentTimeEpisodeProgress.progress = progress
                seekBarCurrentTimeEpisodeProgress.goneIfFalse(currentTime > 0)

                if (currentTime > 0 && duration > 0) {
                    val timeLeft = duration - currentTime
                    textViewItemEpisodeTime.text = binding.root.context.getString(R.string.time_left, "${timeLeft.toHrAndMin()}")
                } else if (duration > 0) {
                    textViewItemEpisodeTime.text = duration.toHrAndMin()
                }

                val seekBarDrawableEnable = ContextCompat.getDrawable(binding.root.context, R.drawable.podcast_episode_progress_bar_duration_holder)
                val seekBarDrawableDisable = ContextCompat.getDrawable(binding.root.context, R.drawable.podcast_episode_disabled_progress_bar_duration_holder)

                // played

                if (podcastEpisode.played) {
                    seekBarCurrentTimeEpisodeProgress.gone
                    buttonCheckMarkPlayed.visible
                    textViewItemEpisodeTime.text = getString(R.string.episode_detail_played_holder)
                } else {
                    buttonCheckMarkPlayed.gone
                 }

                //Playing State
                if (podcastEpisode.playing) {
                    layoutConstraintAlpha.visible

                    buttonPlayEpisode.setImageDrawable(
                        ContextCompat.getDrawable(binding.root.context, R.drawable.ic_pause_episode)
                    )

                    seekBarCurrentTimeEpisodeProgress.progressDrawable = seekBarDrawableEnable
                    textViewEpisodeHeader.setTextColor(ContextCompat.getColor(root.context, R.color.receivedIcon))
                } else {
                    layoutConstraintAlpha.gone

                    buttonPlayEpisode.setImageDrawable(
                        ContextCompat.getDrawable(binding.root.context, R.drawable.ic_play_episode)
                    )

                    seekBarCurrentTimeEpisodeProgress.progressDrawable = seekBarDrawableDisable
                    textViewEpisodeHeader.setTextColor(ContextCompat.getColor(root.context, R.color.primaryText))
                }

                // Image
                podcastEpisode.imageUrlToShow?.value?.let { episodeImage ->
                    lifecycleOwner.lifecycleScope.launch(viewModel.dispatchers.mainImmediate) {
                        disposable = imageLoader.load(
                            imageViewEpisodeImage,
                            episodeImage,
                            imageLoaderOptions
                        )
                    }
                }
                imageViewItemRowEpisodeType.setImageDrawable(ContextCompat.getDrawable(root.context, R.drawable.ic_podcast_type))

                //Download
                val episodeAvailable = (connectivityHelper.isNetworkConnected() || podcastEpisode.downloaded)
                root.alpha = if (episodeAvailable) 1.0f else 0.5f

                if (podcastEpisode.downloaded) {
                    imageDownloadedEpisodeArrow.visible
                    buttonDownloadArrow.gone
                    progressBarEpisodeDownload.gone
                    buttonStop.gone
                }

                if (!podcastEpisode.downloaded) {
                    buttonDownloadArrow.visible
                    buttonStop.gone
                    progressBarEpisodeDownload.gone
                    imageDownloadedEpisodeArrow.gone
                }

                val isFeedItemDownloadInProgress = viewModel.isFeedItemDownloadInProgress(podcastEpisode.id) && !podcastEpisode.downloaded

                if (isFeedItemDownloadInProgress) {
                    buttonDownloadArrow.gone
                    progressBarEpisodeDownload.visible
                    imageDownloadedEpisodeArrow.gone
                    buttonStop.visible
                }

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    delay(100L)

                    if (viewModel.isEpisodeSoundPlaying(podcastEpisode)) {
                        animationViewPlay.playAnimation()
                    } else {
                        animationViewPlay.pauseAnimation()
                    }
                }
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}

private fun getSeekbarProgress(duration: Int, currentTime: Int): Int {
    return try {
        currentTime * 100 / duration
    } catch (e: ArithmeticException) {
        0
    }
}
@Suppress("NOTHING_TO_INLINE")
inline fun Int.toHrAndMin(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60

    return if (hours > 0) {
        "$hours hr $minutes min"
    } else "$minutes min"
}