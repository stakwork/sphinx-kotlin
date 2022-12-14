package chat.sphinx.podcast_player.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
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
import chat.sphinx.podcast_player.databinding.LayoutEpisodeListItemHolderBinding
import chat.sphinx.podcast_player.ui.PodcastPlayerViewModel
import chat.sphinx.podcast_player.ui.viewstates.PodcastPlayerViewState
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_podcast.PodcastEpisode
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
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

                if (viewState is PodcastPlayerViewState.PodcastLoaded) {
                    episodes = viewState.podcast.getEpisodesListCopy()
                }

                if (viewState is PodcastPlayerViewState.EpisodePlayed) {
                    episodes = viewState.podcast.getEpisodesListCopy()
                }

                if (viewState is PodcastPlayerViewState.MediaStateUpdate) {
                    if (viewState.state is MediaPlayerServiceState.ServiceActive.MediaState.Paused ||
                        viewState.state is MediaPlayerServiceState.ServiceActive.MediaState.Ended)
                    {
                        episodes = viewState.podcast.getEpisodesListCopy()
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
                                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                                podcastEpisodes.clear()
                                podcastEpisodes.addAll(episodes)
                                result.dispatchUpdatesTo(this@PodcastEpisodesListAdapter)

                                if (
                                    firstVisibleItemPosition == 0                               &&
                                    recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE
                                ) {
                                    recyclerView.scrollToPosition(0)
                                }
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
        val binding = LayoutEpisodeListItemHolderBinding.inflate(
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
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .build()
    }

    inner class EpisodeViewHolder(
        private val binding: LayoutEpisodeListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var disposable: Disposable? = null
        private var episode: PodcastEpisode? = null

        init {
            binding.layoutConstraintEpisodeListItemHolder.setOnClickListener {
                episode?.let { podcastEpisode ->
                    if (connectivityHelper.isNetworkConnected() || podcastEpisode.downloaded) {
                        viewModel.playEpisodeFromList(podcastEpisode, 0)
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

                val episodeAvailable = (connectivityHelper.isNetworkConnected() || podcastEpisode.downloaded)
                root.alpha = if (episodeAvailable) 1.0f else 0.5f

                swipeRevealLayoutPodcastFeedItem.setLockDrag(!podcastEpisode.downloaded)
                layoutConstraintDeleteButtonContainer.setOnClickListener {
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        viewModel.deleteDownloadedMedia(podcastEpisode)
                        notifyItemChanged(position)
                        swipeRevealLayoutPodcastFeedItem.close(true)
                    }
                }
                //Playing State
                layoutConstraintEpisodeListItemHolder.setBackgroundColor(
                    root.context.getColor(
                        if (podcastEpisode.playing) R.color.chatListSelected else R.color.headerBG
                    )
                )
                textViewPlayArrowIndicator.goneIfFalse(podcastEpisode.playing)

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

                //Name
                textViewEpisodeTitle.text = podcastEpisode.title.value

                //Download button
                textViewDownloadEpisodeButton.setTextColor(
                    root.context.getColor(
                        if (podcastEpisode.downloaded) R.color.primaryGreen else R.color.secondaryText
                    )
                )

                textViewDownloadEpisodeButton.setOnClickListener {
                    viewModel.downloadMedia(podcastEpisode) { downloadedFile ->
                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                            podcastEpisode.localFile = downloadedFile
                            notifyItemChanged(position)
                        }
                    }
                    notifyItemChanged(position)
                }

                val isFeedItemDownloadInProgress = viewModel.isFeedItemDownloadInProgress(podcastEpisode.id) && !podcastEpisode.downloaded

                textViewDownloadEpisodeButton.goneIfTrue(isFeedItemDownloadInProgress)
                progressBarEpisodeDownload.goneIfFalse(isFeedItemDownloadInProgress)
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}
