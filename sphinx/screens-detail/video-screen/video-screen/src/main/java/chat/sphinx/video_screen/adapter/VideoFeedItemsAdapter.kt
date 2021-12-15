package chat.sphinx.video_screen.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.databinding.LayoutVideoListItemHolderBinding
import chat.sphinx.video_screen.ui.VideoFeedScreenViewModel
import chat.sphinx.video_screen.ui.viewstate.VideoFeedScreenViewState
import chat.sphinx.wrapper_common.feed.isYoutubeVideo
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class VideoFeedItemsAdapter (
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: VideoFeedScreenViewModel,
    private val viewModelDispatcher: CoroutineDispatchers,
): RecyclerView.Adapter<VideoFeedItemsAdapter.VideoEpisodeViewHolder>(), DefaultLifecycleObserver {

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
                    old.id                   == new.id                &&
                            old.enclosureUrl              == new.enclosureUrl

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

    private val videoItems = ArrayList<FeedItem>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModelDispatcher.mainImmediate) {

            viewModel.collectViewState { viewState ->

                var items: List<FeedItem> = listOf()

                if (viewState is VideoFeedScreenViewState.FeedLoaded) {
                    items = viewState.items.toList()
                }

                if (items.isNotEmpty()) {
                    if (videoItems.isEmpty()) {
                        videoItems.addAll(items)
                        this@VideoFeedItemsAdapter.notifyDataSetChanged()
                    } else {

                        val diff = Diff(videoItems, items)

                        withContext(viewModel.dispatchers.default) {
                            DiffUtil.calculateDiff(diff)
                        }.let { result ->

                            if (!diff.sameList) {
                                videoItems.clear()
                                videoItems.addAll(items)
                                result.dispatchUpdatesTo(this@VideoFeedItemsAdapter)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return videoItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoEpisodeViewHolder {
        val binding = LayoutVideoListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return VideoEpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoEpisodeViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_video_placeholder)
            .build()
    }

    inner class VideoEpisodeViewHolder(
        private val binding: LayoutVideoListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var videoItem: FeedItem? = null

        init {
            binding.root.setOnClickListener {
                videoItem?.let { nnVideoItem ->
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.videoItemSelected(nnVideoItem)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val f: FeedItem = videoItems.getOrNull(position) ?: let {
                    videoItem = null
                    return
                }
                videoItem = f
                disposable?.dispose()
                holderJob?.cancel()

                f.thumbnailUrlToShow?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModelDispatcher.mainImmediate) {
                        imageLoader.load(
                            imageViewVideoImage,
                            imageUrl.value,
                            imageLoaderOptions
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJob = job
                    }
                }

                textViewVideoTitle.text = f.titleToShow
                textViewVideoDate.text = f.datePublished?.hhmmElseDate()

                textViewDownloadVideoButton.goneIfTrue(
                    f.enclosureUrl.isYoutubeVideo() || f.downloaded
                )

                if (f.downloaded) {
                    // TODO: Add swipe to delete functionality...
                } else {
                    textViewDownloadVideoButton.setOnClickListener {
                        viewModel.downloadMedia(f)
                    }
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
}