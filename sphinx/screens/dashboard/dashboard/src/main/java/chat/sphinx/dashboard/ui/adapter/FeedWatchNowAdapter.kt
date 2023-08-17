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
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Date

class FeedWatchNowAdapter(
    private val recyclerView: RecyclerView,
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

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.feedsHolderViewStateFlow.collect { list ->

                val videosList = mutableListOf<FeedItem>()

                list.forEach { feed ->
                    feed.lastPublished?.let { feedItem ->
                        videosList.add(feedItem)
                    }
                }

                if (videoItems.isEmpty()) {
                    videoItems.addAll(videosList)
                    this@FeedWatchNowAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(videoItems, videosList)

                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            videoItems.clear()
                            videoItems.addAll(videosList)
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

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var video: FeedItem? = null

        init {
            val rowWidth = (recyclerView.rootView.measuredWidth.toFloat() * 0.8).toInt()

            if (binding.imageViewItemImage.layoutParams.width != rowWidth) {
                binding.imageViewItemImage.layoutParams.width = rowWidth
                binding.imageViewItemImage.requestLayout()
            }

            binding.layoutConstraintFeedHolder.setOnClickListener {
                video?.let { nnVideo ->
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.videoItemSelected(nnVideo)
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

                for (job in holderJobs) {
                    job.cancel()
                }

                for (disposable in disposables) {
                    disposable.dispose()
                }

                videoItem.itemImageUrlToShow?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewItemImage,
                            imageUrl.value,
                            imageLoaderOptions
                        ).also {
                            disposables.add(it)
                        }
                    }.let { job ->
                        holderJobs.add(job)
                    }
                } ?: run {
                    imageViewItemImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, R.drawable.ic_video_placeholder)
                    )
                }

                textViewItemName.text = videoItem.titleToShow
                textViewContributorName.text = videoItem.author?.value ?: videoItem.feed?.generator?.value
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