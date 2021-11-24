package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.LayoutPodcastSearchRowHolderBinding
import chat.sphinx.dashboard.databinding.LayoutPodcastSearchSectionHeaderHolderBinding
import chat.sphinx.dashboard.ui.feed.FeedViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
import chat.sphinx.wrapper_podcast.PodcastSearchResult
import chat.sphinx.wrapper_podcast.PodcastSearchResultRow
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PodcastSearchAdapter(
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
        private val oldList: List<PodcastSearchResultRow>,
        private val newList: List<PodcastSearchResultRow>,
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
                    old.podcastSearchResult != null && new.podcastSearchResult != null -> {
                        old.podcastSearchResult?.id == new.podcastSearchResult?.id
                    }
                    old.sectionTitle != null && new.sectionTitle != null -> {
                        old.sectionTitle            == new.sectionTitle
                    }
                    else -> {
                        false
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
                    old.podcastSearchResult != null && new.podcastSearchResult != null -> {
                        old.podcastSearchResult?.id == new.podcastSearchResult?.id
                    }
                    old.sectionTitle != null && new.sectionTitle != null -> {
                        old.sectionTitle            == new.sectionTitle
                    }
                    else -> {
                        false
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

    private val searchResults = ArrayList<PodcastSearchResultRow>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->

                var list: List<PodcastSearchResultRow> = if (viewState is FeedViewState.SearchResults) {
                    viewState.searchResults
                } else {
                    listOf()
                }

                if (searchResults.isEmpty()) {
                    searchResults.addAll(list)
                    this@PodcastSearchAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(searchResults, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            searchResults.clear()
                            searchResults.addAll(list)
                            result.dispatchUpdatesTo(this@PodcastSearchAdapter)
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
        if (searchResults.getOrNull(position)?.podcastSearchResult != null) {
            return CONTENT_VIEW
        }
        return SECTION_VIEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == CONTENT_VIEW) {
            val binding = LayoutPodcastSearchRowHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            return PodcastSearchItemViewHolder(binding)
        }
        val binding = LayoutPodcastSearchSectionHeaderHolderBinding.inflate(
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
            (holder as PodcastSearchItemViewHolder)?.bind(position)
        }
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .build()
    }

    inner class SectionHeaderViewHolder(
        private val binding: LayoutPodcastSearchSectionHeaderHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var sectionViewTitle: String? = null

        fun bind(position: Int) {
            binding.apply {
                val title: String = searchResults.getOrNull(position)?.sectionTitle ?: let {
                    sectionViewTitle = null
                    return
                }
                sectionViewTitle = title

                textViewSectionName.text = title
            }
        }

        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }

    }

    inner class PodcastSearchItemViewHolder(
        private val binding: LayoutPodcastSearchRowHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var searchResult: PodcastSearchResult? = null

        init {
            binding.layoutConstraintSearchResultsHolder.setOnClickListener {
                searchResult?.let { nnSearchResult ->
                    searchResultsSelected(nnSearchResult)
                }
            }
        }

        fun searchResultsSelected(searchResult: PodcastSearchResult) {
            binding.progressBarResultLoading.visible
            binding.layoutConstraintSearchResultsHolder.isClickable = false

            viewModel.podcastSearchResultSelected(searchResult) {
                binding.layoutConstraintSearchResultsHolder.isClickable = true
                binding.progressBarResultLoading.gone
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val resultRow: PodcastSearchResultRow = searchResults.getOrNull(position) ?: let {
                    return
                }
                val result: PodcastSearchResult = resultRow?.podcastSearchResult ?: let {
                    searchResult = null
                    return
                }
                searchResult = result

                disposable?.dispose()
                holderJob?.cancel()

                result.imageUrl?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewPodcastImage,
                            imageUrl,
                            imageLoaderOptions
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJob = job
                    }
                }

                textViewPodcastName.text = result.title
                textViewPodcastDescription.text = result.description

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