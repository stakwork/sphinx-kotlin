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
import chat.sphinx.concept_network_query_podcast_search.model.PodcastSearchResultDto
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.LayoutPodcastSearchRowHolderBinding
import chat.sphinx.dashboard.ui.feed.FeedViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
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
): RecyclerView.Adapter<PodcastSearchAdapter.PodcastSearchItemViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<PodcastSearchResultDto>,
        private val newList: List<PodcastSearchResultDto>,
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
                    old.title                   == new.title                &&
                    old.title                   == new.title

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

    private val searchResults = ArrayList<PodcastSearchResultDto>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->

                var list: List<PodcastSearchResultDto> = if (viewState is FeedViewState.SearchResults) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastSearchAdapter.PodcastSearchItemViewHolder {
        val binding = LayoutPodcastSearchRowHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PodcastSearchItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PodcastSearchAdapter.PodcastSearchItemViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .build()
    }

    inner class PodcastSearchItemViewHolder(
        private val binding: LayoutPodcastSearchRowHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var searchResult: PodcastSearchResultDto? = null

        init {
            binding.layoutConstraintSearchResultsHolder.setOnClickListener {
                searchResult?.let { nnSearchResult ->
                    lifecycleOwner.lifecycleScope.launch {
//                        viewModel.feedSelected(nnFeed)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val result: PodcastSearchResultDto = searchResults.getOrNull(position) ?: let {
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