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
import chat.sphinx.video_screen.ui.VideoScreenViewModel
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_feed.FeedAuthor
import chat.sphinx.wrapper_feed.FeedDescription
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.FeedTitle
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Date

internal class VideoFeedItemsAdapter (
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: VideoScreenViewModel,
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

    private val videoEpisodes = ArrayList<FeedItem>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModelDispatcher.mainImmediate) {
            viewModel.feedItemsHolderViewStateFlow.collect { list ->

                val episodesList = mutableListOf(
                    // Normal Video
                    FeedItem(
                        FeedId("feedItemId"),
                        FeedTitle("Something we see a lot"),
                        FeedDescription("Describing the things we see"),
                        DateTime(Date.valueOf("2021-09-22")),
                        DateTime(Date.valueOf("2021-09-22")),
                        FeedAuthor("Kgothatso"),
                        null,
                        null,
                        FeedUrl("https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4"),
                        null,
                        PhotoUrl("https://pbs.twimg.com/media/FEvdQm5XoAAcXgw?format=jpg&name=small"),
                        PhotoUrl("https://pbs.twimg.com/media/FEvdQm5XoAAcXgw?format=jpg&name=small"),
                        FeedUrl("https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4"),
                        FeedId("feedId"),
                    ),
                    // Youtube Video
                    FeedItem(
                        FeedId("feedYoutubeItemId"),
                        FeedTitle("Youtube we see a lot"),
                        FeedDescription("Describing the things we see"),
                        DateTime(Date.valueOf("2021-09-22")),
                        DateTime(Date.valueOf("2021-09-22")),
                        FeedAuthor("Youtube Channel"),
                        null,
                        null,
                        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                        null,
                        PhotoUrl("https://cdn.mos.cms.futurecdn.net/8gzcr6RpGStvZFA2qRt4v6.jpg"),
                        PhotoUrl("https://cdn.mos.cms.futurecdn.net/8gzcr6RpGStvZFA2qRt4v6.jpg"),
                        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                        FeedId("youtubeFeedId"),
                    )
                )

                list.forEach { feedItem ->
                    episodesList.add(feedItem)
                }

                if (videoEpisodes.isEmpty()) {
                    videoEpisodes.addAll(episodesList)

                    this@VideoFeedItemsAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(videoEpisodes, episodesList)

                    withContext(viewModelDispatcher.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            videoEpisodes.clear()
                            videoEpisodes.addAll(episodesList)
                            result.dispatchUpdatesTo(this@VideoFeedItemsAdapter)
                        }
                    }
                }
            }
        }
    }

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