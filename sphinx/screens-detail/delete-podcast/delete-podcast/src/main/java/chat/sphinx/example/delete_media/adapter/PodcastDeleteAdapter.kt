package chat.sphinx.example.delete_media.adapter

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
import chat.sphinx.delete.media.R
import chat.sphinx.delete.media.databinding.StorageElementListItemHolderBinding
import chat.sphinx.example.delete_media.model.PodcastToDelete
import chat.sphinx.example.delete_media.ui.DeletePodcastViewModel
import chat.sphinx.example.delete_media.viewstate.DeletePodcastViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PodcastDeleteAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: DeletePodcastViewModel,
): RecyclerView.Adapter<PodcastDeleteAdapter.MediaSectionViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<PodcastToDelete>,
        private val newList: List<PodcastToDelete>,
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
                    old.title == new.title

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
                    old.title  == new.title

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

    private val sectionItems = ArrayList<PodcastToDelete>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.viewStateContainer.collect { viewState ->

                var list: List<PodcastToDelete> = if (viewState is DeletePodcastViewState.SectionList) {
                    viewState.section
                } else {
                    listOf()
                }

                if (sectionItems.isEmpty()) {
                    sectionItems.addAll(list)
                    this@PodcastDeleteAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(sectionItems, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            sectionItems.clear()
                            sectionItems.addAll(list)
                            result.dispatchUpdatesTo(this@PodcastDeleteAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return sectionItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastDeleteAdapter.MediaSectionViewHolder {
        val binding = StorageElementListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MediaSectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PodcastDeleteAdapter.MediaSectionViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_podcast_type)
            .build()
    }

    inner class MediaSectionViewHolder(
        private val binding: StorageElementListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var section: PodcastToDelete? = null

        init {
            binding.root.setOnClickListener {
                lifecycleOwner.lifecycleScope.launch {
                    section?.let { section ->
                        viewModel.navigator.toDeleteMediaDetail(section.feedId)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val sectionItem: PodcastToDelete = sectionItems.getOrNull(position) ?: let {
                    section = null
                    return
                }
                section = sectionItem

                sectionItem.image.let { imageUrl ->
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
                } ?: run {
                    imageViewElementPicture.setImageDrawable(
                        ContextCompat.getDrawable(root.context, R.drawable.ic_tribe)
                    )
                }
                textViewManageStorageElementText.text = section?.title
                textViewManageStorageElementNumber.text = section?.size

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