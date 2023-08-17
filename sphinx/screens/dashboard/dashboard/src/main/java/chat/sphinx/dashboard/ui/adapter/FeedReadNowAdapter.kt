package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Transformations
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.LayoutFeedReadNowRowHolderBinding
import chat.sphinx.dashboard.ui.feed.read.FeedReadViewModel
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedReadNowAdapter(
    private val recyclerView: RecyclerView,
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: FeedReadViewModel,
): RecyclerView.Adapter<FeedReadNowAdapter.NewsletterItemViewHolder>(), DefaultLifecycleObserver {

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

    private val newsletterItems = ArrayList<FeedItem>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.feedsHolderViewStateFlow.collect { list ->

                val episodesList = mutableListOf<FeedItem>()

                list.forEach { feed ->
                    feed.lastPublished?.let { feedItem ->
                        episodesList.add(feedItem)
                    }
                }

                if (newsletterItems.isEmpty()) {
                    newsletterItems.addAll(episodesList)
                    this@FeedReadNowAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(newsletterItems, episodesList)

                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            newsletterItems.clear()
                            newsletterItems.addAll(episodesList)
                            result.dispatchUpdatesTo(this@FeedReadNowAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return newsletterItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedReadNowAdapter.NewsletterItemViewHolder {
        val binding = LayoutFeedReadNowRowHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return NewsletterItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedReadNowAdapter.NewsletterItemViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .build()
    }

    private val thumbnailLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    inner class NewsletterItemViewHolder(
        private val binding: LayoutFeedReadNowRowHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var newsletterItem: FeedItem? = null

        init {
            val rowWidth = (recyclerView.rootView.measuredWidth.toFloat() * 0.8).toInt()

            if (binding.imageViewItemImage.layoutParams.width != rowWidth) {
                binding.imageViewItemImage.layoutParams.width = rowWidth
                binding.imageViewItemImage.requestLayout()
            }

            binding.layoutConstraintFeedHolder.setOnClickListener {
                newsletterItem?.let { nnNewsLetterItem ->
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.newsletterItemSelected(nnNewsLetterItem)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val newsletterItem: FeedItem = newsletterItems.getOrNull(position) ?: let {
                    newsletterItem = null
                    return
                }
                this@NewsletterItemViewHolder.newsletterItem = newsletterItem

                for (job in holderJobs) {
                    job.cancel()
                }

                for (disposable in disposables) {
                    disposable.dispose()
                }

                newsletterItem.imageUrlToShow?.let { imageUrl ->
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
                        ContextCompat.getDrawable(root.context, R.drawable.ic_newsletter_placeholder)
                    )
                }

                newsletterItem.thumbnailUrlToShow?.let { thumbnailUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewContributorImage,
                            thumbnailUrl.value,
                            thumbnailLoaderOptions
                        ).also {
                            disposables.add(it)
                        }
                    }.let { job ->
                        holderJobs.add(job)
                    }
                }
                
                textViewItemName.text = newsletterItem.titleToShow
                textViewItemDescription.text = newsletterItem.descriptionToShow

                textViewEntryTimestamp.text = newsletterItem.datePublished?.hhmmElseDate()

                val hasAuthor = newsletterItem.author != null && newsletterItem.author?.value?.isNotEmpty() == true
                textViewContributorName.text = newsletterItem.author?.value
                textViewContributorName.goneIfFalse(hasAuthor)
                textViewDivider.goneIfFalse(hasAuthor)
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