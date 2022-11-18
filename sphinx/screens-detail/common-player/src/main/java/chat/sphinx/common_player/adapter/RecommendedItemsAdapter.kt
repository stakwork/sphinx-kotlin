package chat.sphinx.common_player.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.common_player.databinding.LayoutRecommendedListItemHolderBinding
import chat.sphinx.common_player.ui.CommonPlayerScreenViewModel
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class RecommendedItemsAdapter (
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: CommonPlayerScreenViewModel,
    private val viewModelDispatcher: CoroutineDispatchers,
): RecyclerView.Adapter<RecommendedItemsAdapter.RecommendedItemViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<FeedItem>,
        private val newList: List<FeedItem>,
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
                    old.id                   == new.id                &&
                            old.enclosureUrl              == new.enclosureUrl

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
    private val recommendedItems = ArrayList<FeedItem>(listOf())

    inner class RecommendedItemViewHolder(
        private val binding: LayoutRecommendedListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)

        private var recommendedItem: FeedItem? = null

        init {
            binding.layoutConstraintRecommendedHolder.setOnClickListener {
                recommendedItem?.let { nnRecommendedItem->
                    lifecycleOwner.lifecycleScope.launch {
                        // Evaluate which type of item, and set the correct player for it.
                    }
                }
            }
        }

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendedItemViewHolder {
        val binding = LayoutRecommendedListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return RecommendedItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecommendedItemViewHolder, position: Int) {
    }

    override fun getItemCount(): Int {
        return 10
    }

}