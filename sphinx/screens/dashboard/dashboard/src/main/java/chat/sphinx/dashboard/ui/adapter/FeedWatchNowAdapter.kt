package chat.sphinx.dashboard.ui.adapter

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
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.LayoutFeedWatchNowRowHolderBinding
import chat.sphinx.dashboard.ui.feed.watch.FeedWatchViewModel
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_feed.*
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Date

class FeedWatchNowAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: FeedWatchViewModel,
): RecyclerView.Adapter<FeedWatchNowAdapter.VideoViewHolder>(), DefaultLifecycleObserver {

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

    private val videoItems = ArrayList<FeedItem>(listOf())

    private val youtubeFeed = Feed(
        FeedId("feedYoutubeItemId"),
        FeedType.Video,
        FeedTitle("Youtube we see a lot"),
        FeedDescription("Describing the things we see"),
        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
        FeedAuthor("Youtube Channel"),
        null,
        PhotoUrl("https://cdn.mos.cms.futurecdn.net/8gzcr6RpGStvZFA2qRt4v6.jpg"),
        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
        DateTime(Date.valueOf("2021-09-22")),
        DateTime(Date.valueOf("2021-09-22")),
        null,
        null,
        FeedItemsCount(0L),
        null,
        ChatId(0L),
    )

    private val remoteVideoFeed = Feed(
        FeedId("feedItemId"),
        FeedType.Video,
        FeedTitle("Youtube we see a lot"),
        FeedDescription("Describing the things we see"),
        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
        FeedAuthor("Normal Video Feed"),
        null,
        PhotoUrl("https://pbs.twimg.com/media/FEvdQm5XoAAcXgw?format=jpg&name=small"),
        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
        DateTime(Date.valueOf("2021-09-22")),
        DateTime(Date.valueOf("2021-09-22")),
        null,
        null,
        FeedItemsCount(0L),
        null,
        ChatId(0L),
    )

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.feedsHolderViewStateFlow.collect { list ->

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
                    ).apply {
                            this.feed = remoteVideoFeed
                    },
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
                    ).apply {
                        this.feed = youtubeFeed
                    }
                )

                list.forEach { feed ->
                    feed.lastItem?.let { feedItem ->
                        episodesList.add(feedItem)
                    }
                }

                if (videoItems.isEmpty()) {
                    videoItems.addAll(episodesList)
                    this@FeedWatchNowAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(videoItems, episodesList)

                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            videoItems.clear()
                            videoItems.addAll(episodesList)
                            result.dispatchUpdatesTo(this@FeedWatchNowAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return videoItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedWatchNowAdapter.VideoViewHolder {
        val binding = LayoutFeedWatchNowRowHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedWatchNowAdapter.VideoViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_video_placeholder)
            .build()
    }

    inner class VideoViewHolder(
        private val binding: LayoutFeedWatchNowRowHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var video: FeedItem? = null

        init {
            binding.layoutConstraintFeedHolder.setOnClickListener {
                video?.let { nnVideo ->
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.episodeItemSelected(nnVideo)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val videoItem: FeedItem = videoItems.getOrNull(position) ?: let {
                    video = null
                    return
                }
                video = videoItem
                disposable?.dispose()
                holderJob?.cancel()

                videoItem.imageUrlToShow?.let { imageUrl ->
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
                }

                videoItem.feed?.imageUrlToShow?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewContributorImage,
                            imageUrl.value,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_tribe)
                                .build()
                        ).also {
                            // TODO: disposable array
//                            disposable = it
                        }
                    }.let { job ->
                        // TODO: holderJob array
//                        holderJob = job
                    }
                }

                textViewItemName.text = videoItem.titleToShow
                textViewContributorName.text = videoItem.author?.value
                textViewEntryTimestamp.text = videoItem.datePublished?.hhmmElseDate()
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