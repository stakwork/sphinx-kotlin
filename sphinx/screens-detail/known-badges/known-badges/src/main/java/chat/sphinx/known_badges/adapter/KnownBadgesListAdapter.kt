package chat.sphinx.known_badges.adapter

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
import chat.sphinx.known_badges.R
import chat.sphinx.known_badges.databinding.LayoutKnownBadgesListItemHolderBinding
import chat.sphinx.known_badges.ui.KnownBadgesViewModel
import chat.sphinx.known_badges.ui.KnownBadgesViewState
import chat.sphinx.wrapper_badge.Badge
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class KnownBadgesListAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: KnownBadgesViewModel,
):
    RecyclerView.Adapter<KnownBadgesListAdapter.KnownBadgeViewHolder>(),
    DefaultLifecycleObserver
{

    private inner class Diff(
        private val oldList: List<Badge>,
        private val newList: List<Badge>,
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
                    old.name                  == new.name            &&
                    old.imageUrl              == new.imageUrl


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
                            old.name           == new.name      &&
                            old.imageUrl       == new.imageUrl

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

    private val knownBadges = ArrayList<Badge>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->

                var list: List<Badge> = if (viewState is KnownBadgesViewState.KnownBadges) {
                    viewState.badges
                } else {
                    listOf()
                }

                if (knownBadges.isEmpty()) {
                    knownBadges.addAll(list)
                    this@KnownBadgesListAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(knownBadges, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            knownBadges.clear()
                            knownBadges.addAll(list)
                            result.dispatchUpdatesTo(this@KnownBadgesListAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return knownBadges.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KnownBadgeViewHolder {
        val binding = LayoutKnownBadgesListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return KnownBadgeViewHolder(binding)

    }

    override fun onBindViewHolder(holder: KnownBadgeViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.sphinx_icon)
            .build()
    }

    inner class KnownBadgeViewHolder(
        private val binding: LayoutKnownBadgesListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)
        fun bind(position: Int) {
            binding.apply {
                val badge: Badge? = knownBadges.getOrNull(position)

                badge?.imageUrl?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewBadgeImage,
                            imageUrl,
                            imageLoaderOptions
                        ).also {
                            disposables.add(it)
                        }
                    }.let { job ->
                        holderJobs.add(job)
                    }
                } ?: run {
                    imageViewBadgeImage.setImageDrawable(
                        ContextCompat.getDrawable(root.context, R.drawable.ic_tribe)
                    )
                }

                textViewBadgeTitle.text = badge?.name ?: ""
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}
















