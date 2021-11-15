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
import chat.sphinx.dashboard.databinding.LayoutFeedReadNowRowHolderBinding
import chat.sphinx.dashboard.ui.feed.read.FeedReadViewModel
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedReadNowAdapter(
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
                    feed.lastItem?.let { feedItem ->
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

    inner class NewsletterItemViewHolder(
        private val binding: LayoutFeedReadNowRowHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var newsletterItem: FeedItem? = null

        init {
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
                disposable?.dispose()
                holderJob?.cancel()

                newsletterItem.imageUrlToShow?.let { imageUrl ->
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

                textViewItemName.text = newsletterItem.titleToShow
                textViewItemDescription.text = newsletterItem.descriptionToShow
                textViewContributorName.text = newsletterItem.author?.value
                textViewContributorName.text = newsletterItem.datePublished?.hhmmElseDate()
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