package chat.sphinx.newsletter_detail.ui.adapter

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
import chat.sphinx.newsletter_detail.databinding.LayoutItemsListItemHolderBinding
import chat.sphinx.newsletter_detail.ui.NewsletterDetailViewModel
import chat.sphinx.newsletter_detail.ui.NewsletterDetailViewState
import chat.sphinx.newsletter_detail.R
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList


internal class NewsletterItemsListAdapter(
    private val recyclerView: RecyclerView,
    private val layoutManager: LinearLayoutManager,
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: NewsletterDetailViewModel,
): RecyclerView.Adapter<NewsletterItemsListAdapter.ItemViewHolder>(), DefaultLifecycleObserver {

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

                val same: Boolean = old.id             == new.id          &&
                                    old.title          == new.title       &&
                                    old.description    == new.description

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

    private val newsletterItems = ArrayList<FeedItem>()

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->

                var items: List<FeedItem> = listOf()

                if (viewState is NewsletterDetailViewState.FeedLoaded) {
                    items = viewState.items.toList()
                }

                if (items.isNotEmpty()) {
                    if (newsletterItems.isEmpty()) {
                        newsletterItems.addAll(items)
                        this@NewsletterItemsListAdapter.notifyDataSetChanged()
                    } else {

                        val diff = Diff(newsletterItems, items)

                        withContext(viewModel.dispatchers.default) {
                            DiffUtil.calculateDiff(diff)
                        }.let { result ->

                            if (!diff.sameList) {
                                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                                newsletterItems.clear()
                                newsletterItems.addAll(items)
                                result.dispatchUpdatesTo(this@NewsletterItemsListAdapter)

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
        return newsletterItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsletterItemsListAdapter.ItemViewHolder {
        val binding = LayoutItemsListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsletterItemsListAdapter.ItemViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_newsletter_placeholder)
            .build()
    }

    inner class ItemViewHolder(
        private val binding: LayoutItemsListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var disposable: Disposable? = null
        private var holderJob: Job? = null

        private var item: FeedItem? = null

        init {
            binding.layoutConstraintNewsletterListItemHolder.setOnClickListener {
                item?.let { newsletterItem ->
                    viewModel.newsletterItemSelected(newsletterItem)
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val newsletterItem: FeedItem = newsletterItems.getOrNull(position) ?: let {
                    item = null
                    return
                }
                item = newsletterItem

                disposable?.dispose()
                holderJob?.cancel()

                // Image
                newsletterItem.itemImageUrlToShow?.value?.let { itemImage ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewNewsletterImage,
                            itemImage,
                            imageLoaderOptions
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJob = job
                    }
                }

                //Name
                textViewNewsletterTitle.text = newsletterItem.titleToShow
                textViewNewsletterDescription.text = newsletterItem.descriptionToShow
                textViewNewsletterDate.text = newsletterItem.datePublished?.hhmmElseDate()
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}
