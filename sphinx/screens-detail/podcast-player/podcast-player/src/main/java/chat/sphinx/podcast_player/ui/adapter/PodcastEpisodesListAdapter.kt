package chat.sphinx.podcast_player.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.podcast_player.R
import chat.sphinx.podcast_player.databinding.LayoutEpisodeListItemHolderBinding
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.objects.PodcastEpisode
import chat.sphinx.podcast_player.ui.PodcastPlayerViewModel
import chat.sphinx.podcast_player.ui.PodcastPlayerViewState
import chat.sphinx.wrapper_chat.*
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList


internal class PodcastEpisodesListAdapter(
    private val recyclerView: RecyclerView,
    private val layoutManager: LinearLayoutManager,
    private val imageLoader: ImageLoader<ImageView>,
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

                val same: Boolean = old.id == new.id && old.playing == new.playing

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

    private var podcast: Podcast? = null
    private val podcastEpisodes = ArrayList<PodcastEpisode>(viewModel.currentViewState.episodesList)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->

                if (viewState is PodcastPlayerViewState.PodcastLoaded) {
                    podcast = viewState.podcast
                }

                if (podcastEpisodes.isEmpty()) {
                    podcastEpisodes.addAll(viewState.episodesList)
                    this@PodcastEpisodesListAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(podcastEpisodes, viewState.episodesList)

                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                            podcastEpisodes.clear()
                            podcastEpisodes.addAll(viewState.episodesList)
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
                podcast?.let { podcast ->
                    episode?.let { podcastEpisode ->
                        viewModel.playEpisode(podcast, podcastEpisode, 0)
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

                //Playing State
                layoutConstraintEpisodeListItemHolder.setBackgroundColor(
                    root.context.getColor(
                        if (podcastEpisode.playing) R.color.headerBG else R.color.headerBG
                    )
                )
                textViewPlayArrowIndicator.goneIfFalse(podcastEpisode.playing)

                // Image
                podcastEpisode.image.let { url ->
                    if (url != null) {
                        onStopSupervisor.scope.launch(viewModel.dispatchers.mainImmediate) {
                            imageLoader.load(
                                imageViewEpisodeImage,
                                url,
                                imageLoaderOptions
                            )
                        }
                    }
                }

                //Name
                textViewEpisodeTitle.text = podcastEpisode.title

                //Download button
                textViewDownloadEpisodeButton.setTextColor(
                    root.context.getColor(
                        if (podcastEpisode.downloaded) R.color.primaryGreen else android.R.color.white
                    )
                )
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
