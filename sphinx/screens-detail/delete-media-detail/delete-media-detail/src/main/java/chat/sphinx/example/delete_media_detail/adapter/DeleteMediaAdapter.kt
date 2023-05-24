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
import chat.sphinx.delete.media.detail.databinding.MediaStorageListItemHolderBinding
import chat.sphinx.example.delete_media_detail.ui.DeleteMediaDetailViewModel
import chat.sphinx.example.delete_media_detail.viewstate.MediaItemHolderViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class DeleteMediaAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: DeleteMediaDetailViewModel,
): RecyclerView.Adapter<DeleteMediaAdapter.DiscoverTribeViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<MediaItemHolderViewState>,
        private val newList: List<MediaItemHolderViewState>,
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
                    old.mediaItem?.name == new.mediaItem?.name

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
                    old.mediaItem?.name  == new.mediaItem?.name

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

    private val sectionItems = ArrayList<MediaItemHolderViewState>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.viewStateContainer.collect { viewState ->

                sectionItems.clear()

                // Load all sections

//                (viewState as? SectionHolderViewState.Section)?.tribes?.let { list ->
//                    sectionItems.addAll(list)
//                } ?: run {
//                    sectionItems.clear()
//                }

                this@DeleteMediaAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return 5
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeleteMediaAdapter.DiscoverTribeViewHolder {
        val binding = MediaStorageListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DiscoverTribeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeleteMediaAdapter.DiscoverTribeViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_tribe)
            .build()
    }

    inner class DiscoverTribeViewHolder(
        private val binding: MediaStorageListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var section: MediaItemHolderViewState? = null

        init {
            binding.root.setOnClickListener {
                viewModel.openDeleteItemPopUp()
            }
        }

        fun bind(position: Int) {
            binding.apply {

                for (job in holderJobs) {
                    job.cancel()
                }

                for (disposable in disposables) {
                    disposable.dispose()
                }

                val tribeItem: MediaItemHolderViewState = sectionItems.getOrNull(position) ?: let {
                    section = null
                    return
                }
                section = tribeItem
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