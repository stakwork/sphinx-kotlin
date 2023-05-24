package chat.sphinx.example.delete_media.adapter

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
import chat.sphinx.delete.media.R
import chat.sphinx.delete.media.databinding.StorageElementListItemHolderBinding
import chat.sphinx.example.delete_media.ui.DeleteMediaViewModel
import chat.sphinx.example.delete_media.viewstate.SectionHolderViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class MediaSectionAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: DeleteMediaViewModel,
): RecyclerView.Adapter<MediaSectionAdapter.DiscoverTribeViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<SectionHolderViewState>,
        private val newList: List<SectionHolderViewState>,
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
                    old.mediaSection?.name == new.mediaSection?.name

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
                    old.mediaSection?.name  == new.mediaSection?.name

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

    private val sectionItems = ArrayList<SectionHolderViewState>(listOf())

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

                this@MediaSectionAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return 5
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaSectionAdapter.DiscoverTribeViewHolder {
        val binding = StorageElementListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DiscoverTribeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaSectionAdapter.DiscoverTribeViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_tribe)
            .build()
    }

    inner class DiscoverTribeViewHolder(
        private val binding: StorageElementListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var section: SectionHolderViewState? = null

        init {
            binding.root.setOnClickListener {
                // handle navigation
                section?.let { nnTribe ->
                    lifecycleOwner.lifecycleScope.launch {}
                }
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

                val tribeItem: SectionHolderViewState = sectionItems.getOrNull(position) ?: let {
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