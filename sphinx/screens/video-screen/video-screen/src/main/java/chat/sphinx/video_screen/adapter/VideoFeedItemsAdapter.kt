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
import chat.sphinx.video_screen.databinding.LayoutEpisodeListItemHolderBinding
import chat.sphinx.video_screen.ui.Placeholder
import chat.sphinx.video_screen.ui.VideoFeedViewModel
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class VideoFeedItemsAdapter (
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: VideoFeedViewModel,
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

//    private val videoEpisodes = ArrayList<FeedItem>(listOf())
    private val videoEpisodes = mutableListOf(
        Placeholder.remoteVideoFeedItem,
        Placeholder.youtubeFeedItem
    )

//    override fun onStart(owner: LifecycleOwner) {
//        super.onStart(owner)
//
//        onStopSupervisor.scope.launch(viewModelDispatcher.mainImmediate) {
//            viewModel.feedItemsHolderViewStateFlow.collect { list ->
//
//                if (videoEpisodes.isEmpty()) {
//                    videoEpisodes.addAll(list)
//
//                    this@VideoFeedItemsAdapter.notifyDataSetChanged()
//                } else {
//
//                    val diff = Diff(videoEpisodes, list)
//
//                    withContext(viewModelDispatcher.default) {
//                        DiffUtil.calculateDiff(diff)
//                    }.let { result ->
//
//                        if (!diff.sameList) {
//                            videoEpisodes.clear()
//                            videoEpisodes.addAll(list)
//                            result.dispatchUpdatesTo(this@VideoFeedItemsAdapter)
//                        }
//                    }
//                }
//            }
//        }
//    }

    override fun getItemCount(): Int {
        return videoEpisodes.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoEpisodeViewHolder {
        val binding = LayoutEpisodeListItemHolderBinding.inflate(
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
            .placeholderResId(R.drawable.paid_image_blurred_placeholder)
            .build()
    }

    inner class VideoEpisodeViewHolder(
        private val binding: LayoutEpisodeListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var videoEpisode: FeedItem? = null

        init {
            binding.root.setOnClickListener {
                videoEpisode?.let { nnFeed ->
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.episodeSelected(nnFeed)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val f: FeedItem = videoEpisodes.getOrNull(position) ?: let {
                    videoEpisode = null
                    return
                }
                videoEpisode = f
                disposable?.dispose()
                holderJob?.cancel()

                f.imageUrlToShow?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModelDispatcher.mainImmediate) {
                        imageLoader.load(
                            imageViewEpisodeImage,
                            imageUrl.value,
                            imageLoaderOptions
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJob = job
                    }
                }

                textViewEpisodeTitle.text = f.titleToShow
                textViewEpisodePublishedDate.text = f.datePublished?.hhmmElseDate()
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