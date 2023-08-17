package chat.sphinx.example.delete_media_detail.adapter

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
import chat.sphinx.delete.media.detail.R
import chat.sphinx.delete.media.detail.databinding.PodcastStorageListItemHolderBinding
import chat.sphinx.example.delete_media_detail.model.PodcastDetailToDelete
import chat.sphinx.example.delete_media_detail.ui.DeletePodcastDetailViewModel
import chat.sphinx.example.delete_media_detail.viewstate.DeleteMediaDetailViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeletePodcastDetailAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: DeletePodcastDetailViewModel,
): RecyclerView.Adapter<DeletePodcastDetailAdapter.DeleteEpisodeViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<PodcastDetailToDelete>,
        private val newList: List<PodcastDetailToDelete>,
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
                    old.feedItem.id == new.feedItem.id

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
                    old.feedItem.id  == new.feedItem.id

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

    private val episodeItems = ArrayList<PodcastDetailToDelete>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.viewStateContainer.collect { viewState ->

                var list: List<PodcastDetailToDelete> = if (viewState is DeleteMediaDetailViewState.EpisodeList) {
                    viewState.episodes
                } else {
                    listOf()
                }

                if (episodeItems.isEmpty()) {
                    episodeItems.addAll(list)
                    this@DeletePodcastDetailAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(episodeItems, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            episodeItems.clear()
                            episodeItems.addAll(list)
                            result.dispatchUpdatesTo(this@DeletePodcastDetailAdapter)
                        }
                    }
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return episodeItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeletePodcastDetailAdapter.DeleteEpisodeViewHolder {
        val binding = PodcastStorageListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DeleteEpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeletePodcastDetailAdapter.DeleteEpisodeViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_placeholder)
            .build()
    }

    inner class DeleteEpisodeViewHolder(
        private val binding: PodcastStorageListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var episode: PodcastDetailToDelete? = null

        init {
            binding.root.setOnClickListener {
                episode?.let { nnEpisode ->
                viewModel.openDeleteItemPopup(nnEpisode.feedItem)
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val episodeItem: PodcastDetailToDelete = episodeItems.getOrNull(position) ?: let {
                    episode = null
                    return
                }
                episode = episodeItem

                episodeItem.feedItem.imageUrlToShow?.value?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewElementPicture,
                            imageUrl,
                            imageLoaderOptions
                        ).also {
                            disposables.add(it)
                        }
                    }.let { job ->
                        holderJobs.add(job)
                    }
                }
                textViewManageStorageElementText.text = episodeItem.feedItem.titleToShow
                textViewManageStorageElementNumber.text = episodeItem.size
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