package chat.sphinx.chat_tribe.adapters

import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.model.BadgeItem
import chat.sphinx.chat_tribe.ui.BadgesListViewModel
import chat.sphinx.resources.databinding.LayoutTribeProfileBadgesHolderBinding
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.chat_tribe.ui.viewstate.BadgesListViewState
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_network_query_people.model.BadgeDto
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BadgesItemAdapter (
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: BadgesListViewModel
): RecyclerView.Adapter<BadgesItemAdapter.BadgesItemViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<BadgeDto>,
        private val newList: List<BadgeDto>,
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

                val same: Boolean = old.name == new.name

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

                val same: Boolean = old.name == new.name

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

    private val badgeItems = ArrayList<BadgeDto>()

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->

                var badges = ArrayList<BadgeDto>()

                if (viewState is BadgesListViewState.BadgesLoaded) {
                    badges = ArrayList(viewState.badges)
                }

                if (badges.isNotEmpty()) {
                    if (badgeItems.isEmpty()) {
                        badgeItems.addAll(badges)
                        this@BadgesItemAdapter.notifyDataSetChanged()
                    } else {

                        val diff = Diff(badgeItems, badges)

                        withContext(viewModel.dispatchers.default) {
                            DiffUtil.calculateDiff(diff)
                        }.let { result ->

                            if (!diff.sameList) {
                                badgeItems.clear()
                                badgeItems.addAll(badges)
                                result.dispatchUpdatesTo(this@BadgesItemAdapter)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return 5
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgesItemViewHolder {
        val binding = LayoutTribeProfileBadgesHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return BadgesItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgesItemViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class BadgesItemViewHolder(
        private val binding: LayoutTribeProfileBadgesHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var holderJob: Job? = null
        private var disposable: Disposable? = null

        private var badge: BadgeDto? = null

        init {
//            binding.layoutConstraintRecommendedHolder.setOnClickListener {
//                episode?.let { podcastEpisode ->
//                    if (connectivityHelper.isNetworkConnected()) {
//                        viewModel.playEpisodeFromList(podcastEpisode)
//                    }
//                }
//            }
        }

        fun bind(position: Int) {
            binding.apply {

                val badgeItem: BadgeDto = badgeItems.getOrNull(position) ?: let {
                    badge = null
                    return
                }
                badge = badgeItem
                disposable?.dispose()
                holderJob?.cancel()

                badgeItem.icon?.let { imageUrl ->
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewTribeBadgePicture,
                            imageUrl,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_tribe_placeholder)
                                .build()
                        )

                        textViewTribeBadgeName.text = badgeItem.name ?: "-"
                    }
                }
            }
        }
    }
    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}