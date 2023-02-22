package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.LayoutFeedSearchRowHolderBinding
import chat.sphinx.dashboard.databinding.LayoutFeedSearchSectionHeaderHolderBinding
import chat.sphinx.dashboard.ui.feed.FeedViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.toFeedType
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_podcast.FeedSearchResult
import chat.sphinx.wrapper_podcast.FeedSearchResultRow
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedSearchAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: FeedViewModel,
): RecyclerView.Adapter<RecyclerView.ViewHolder>(), DefaultLifecycleObserver {

    companion object {
        private const val SECTION_VIEW = 0
        private const val CONTENT_VIEW = 1
    }

    private inner class Diff(
        private val oldList: List<FeedSearchResultRow>,
        private val newList: List<FeedSearchResultRow>,
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

                val same: Boolean = when {
                    old.feedSearchResult != null && new.feedSearchResult != null -> {
                        old.feedSearchResult?.id == new.feedSearchResult?.id
                    }
                    else -> {
                        old.isSectionHeader      == new.isSectionHeader &&
                        old.isFollowingSection   == new.isFollowingSection
                    }
                }

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

                val same: Boolean = when {
                    old.feedSearchResult != null && new.feedSearchResult != null -> {
                        old.feedSearchResult?.id == new.feedSearchResult?.id
                    }
                    else -> {
                        old.isSectionHeader      == new.isSectionHeader &&
                        old.isFollowingSection   == new.isFollowingSection
                    }
                }

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

    private val searchResults = ArrayList<FeedSearchResultRow>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->

                var list: List<FeedSearchResultRow> = if (viewState is FeedViewState.SearchResults) {
                    viewState.searchResults
                } else {
                    listOf()
                }

                if (searchResults.isEmpty()) {
                    searchResults.addAll(list)
                    this@FeedSearchAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(searchResults, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            searchResults.clear()
                            searchResults.addAll(list)
                            result.dispatchUpdatesTo(this@FeedSearchAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return searchResults.size
    }

    override fun getItemViewType(position: Int): Int {
        if (searchResults.getOrNull(position)?.feedSearchResult != null) {
            return CONTENT_VIEW
        }
        return SECTION_VIEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == CONTENT_VIEW) {
            val binding = LayoutFeedSearchRowHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            return FeedSearchItemViewHolder(binding)
        }
        val binding = LayoutFeedSearchSectionHeaderHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return SectionHeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (SECTION_VIEW == getItemViewType(position)) {
            (holder as SectionHeaderViewHolder)?.bind(position)
        } else {
            (holder as FeedSearchItemViewHolder)?.bind(position)
        }
    }

    private fun imageLoaderOptions(result: FeedSearchResult): ImageLoaderOptions {
        return ImageLoaderOptions.Builder()
            .placeholderResId(result.getPlaceHolderImageRes())
            .build()
    }

    inner class SectionHeaderViewHolder(
        private val binding: LayoutFeedSearchSectionHeaderHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var sectionViewTitle: String? = null

        fun bind(position: Int) {
            binding.apply {
                val title: String = root.context.getString(
                    if (searchResults.getOrNull(position)?.isFollowingSection == true) {
                        R.string.feed_search_following_section
                    } else {
                        R.string.feed_search_directory_section
                    }
                )
                sectionViewTitle = title
                textViewSectionName.text = title
            }
        }

        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }

    }

    inner class FeedSearchItemViewHolder(
        private val binding: LayoutFeedSearchRowHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var searchResult: FeedSearchResult? = null

        init {
            binding.layoutConstraintSearchResultsHolder.setOnClickListener {
                searchResult?.let { nnSearchResult ->
                    searchResultsSelected(nnSearchResult)
                }
            }
        }

        private fun searchResultsSelected(searchResult: FeedSearchResult) {
            binding.progressBarResultLoading.visible
            binding.layoutConstraintSearchResultsHolder.isClickable = false

            viewModel.feedSearchResultSelected(searchResult) {
                binding.layoutConstraintSearchResultsHolder.isClickable = true
                binding.progressBarResultLoading.gone
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val resultRow: FeedSearchResultRow = searchResults.getOrNull(position) ?: let {
                    return
                }
                val result: FeedSearchResult = resultRow?.feedSearchResult ?: let {
                    searchResult = null
                    return
                }
                searchResult = result

                disposable?.dispose()
                holderJob?.cancel()

                result.imageUrl?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewFeedImage,
                            imageUrl,
                            imageLoaderOptions(result)
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJob = job
                    }
                } ?: run {
                    imageViewFeedImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, result.getPlaceHolderImageRes())
                    )
                }

                textViewFeedName.text = result.title
                textViewFeedDescription.text = result.description

                progressBarResultLoading.gone

                viewDivider.goneIfFalse(!resultRow.isLastOnSection)
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

inline fun FeedSearchResult.getPlaceHolderImageRes(): Int =
    when (feedType.toInt().toFeedType()) {
        is FeedType.Podcast -> {
            R.drawable.ic_podcast_placeholder
        }
        is FeedType.Video -> {
            R.drawable.ic_video_placeholder
        }
        is FeedType.Newsletter -> {
            R.drawable.ic_newsletter_placeholder
        }
        else -> {
            R.drawable.ic_podcast_placeholder
        }
    }