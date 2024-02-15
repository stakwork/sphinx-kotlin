package chat.sphinx.tribes_discover.adapter

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
import chat.sphinx.tribes_discover.R
import chat.sphinx.tribes_discover.databinding.LayoutDiscoverTribeListItemHolderBinding
import chat.sphinx.tribes_discover.ui.TribesDiscoverViewModel
import chat.sphinx.resources.getString
import chat.sphinx.tribes_discover.viewstate.DiscoverTribesViewState
import chat.sphinx.tribes_discover.viewstate.TribeHolderViewState
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class TribesDiscoverAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: TribesDiscoverViewModel,
): RecyclerView.Adapter<TribesDiscoverAdapter.DiscoverTribeViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<TribeHolderViewState>,
        private val newList: List<TribeHolderViewState>,
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
                    old.tribeDto?.uuid  == new.tribeDto?.uuid

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
                    old.tribeDto?.uuid  == new.tribeDto?.uuid

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

    private val tribeItems = ArrayList<TribeHolderViewState>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.viewStateContainer.collect { viewState ->

                tribeItems.clear()

                (viewState as? DiscoverTribesViewState.Tribes)?.tribes?.let { list ->
                    tribeItems.addAll(list)
                } ?: run {
                    tribeItems.clear()
                }

                this@TribesDiscoverAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return tribeItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TribesDiscoverAdapter.DiscoverTribeViewHolder {
        val binding = LayoutDiscoverTribeListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DiscoverTribeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TribesDiscoverAdapter.DiscoverTribeViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_tribe)
            .build()
    }

    inner class DiscoverTribeViewHolder(
        private val binding: LayoutDiscoverTribeListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var tribe: TribeHolderViewState? = null

        init {
            binding.root.setOnClickListener {
                tribe?.let { nnTribe ->
                    lifecycleOwner.lifecycleScope.launch {
                        nnTribe.tribeDto?.pubkey?.let { nnPubKey ->
                            viewModel.handleTribeLink(nnPubKey)
                        }
                    }
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

                val tribeItem: TribeHolderViewState = tribeItems.getOrNull(position) ?: let {
                    tribe = null
                    return
                }
                tribe = tribeItem

                tribeItem.tribeDto?.let { tribeDto ->
                    tribeDto.img?.let { imageUrl ->
                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                            imageLoader.load(
                                imageViewTribeImage,
                                imageUrl,
                                imageLoaderOptions
                            ).also {
                                disposables.add(it)
                            }
                        }.let { job ->
                            holderJobs.add(job)
                        }
                    } ?: run {
                        imageViewTribeImage.setImageDrawable(
                            ContextCompat.getDrawable(root.context, R.drawable.ic_tribe)
                        )
                    }

                    textViewTribeTitle.text = tribeDto.name
                    textViewTribeDescription.text = tribeDto.description

                    layoutButtonJoin.apply {
                        if (tribeDto.joined == true) {
                            textViewButtonSmall.text = getString(R.string.discover_tribes_open)
                            layoutConstraintButtonSmall.background = ContextCompat.getDrawable(root.context, R.drawable.background_button_open)
                        } else {
                            textViewButtonSmall.text = getString(R.string.discover_tribes_join)
                            layoutConstraintButtonSmall.background = ContextCompat.getDrawable(root.context, R.drawable.background_button_join)
                        }
                    }
                }

                includeLoadingMoreTribes.root.goneIfFalse(
                    (tribeItem is TribeHolderViewState.Loader)
                )

                layoutConstraintItemContainer.goneIfFalse(
                    (tribeItem is TribeHolderViewState.Tribe)
                )
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