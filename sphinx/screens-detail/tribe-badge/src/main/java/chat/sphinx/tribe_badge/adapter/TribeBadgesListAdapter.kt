package chat.sphinx.tribe_badge.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.resources.getString
import chat.sphinx.tribe_badge.R
import chat.sphinx.tribe_badge.databinding.LayoutBadgesListItemHolderBinding
import chat.sphinx.tribe_badge.databinding.LayoutManageBadgesLabelItemHolderBinding
import chat.sphinx.tribe_badge.model.TribeBadge
import chat.sphinx.tribe_badge.ui.TribeBadgesViewModel
import chat.sphinx.tribe_badge.ui.TribeBadgesViewState
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TribeBadgesListAdapter(
    private val recyclerView: RecyclerView,
    private val layoutManager: LinearLayoutManager,
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: TribeBadgesViewModel,
):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    DefaultLifecycleObserver
{
    companion object {
        private const val TEMPLATE_BADGE = 0
        private const val EXISTING_BADGE = 1
        private const val MANAGE_LABEL = 2
    }

    private inner class Diff(
        private val oldList: List<TribeBadge>,
        private val newList: List<TribeBadge>,
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
                    old.name                  == new.name


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
                            old.name                             == new.name                        &&
                            old.description                      == new.description                 &&
                            old.imageUrl                            == new.imageUrl

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

    private val tribeBadgesList = ArrayList<TribeBadge>(listOf())

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->

                var list: List<TribeBadge> = if (viewState is TribeBadgesViewState.TribeBadgesList) {
                    viewState.tribeBadges
                } else {
                    listOf()
                }

                if (tribeBadgesList.isEmpty()) {
                    tribeBadgesList.addAll(list)
                    this@TribeBadgesListAdapter.notifyDataSetChanged()
                } else {

                    val diff = Diff(tribeBadgesList, list)

                    withContext(viewModel.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            tribeBadgesList.clear()
                            tribeBadgesList.addAll(list)
                            result.dispatchUpdatesTo(this@TribeBadgesListAdapter)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return tribeBadgesList.size
    }

    override fun getItemViewType(position: Int): Int {
        val badgeItemType = tribeBadgesList.getOrNull(position)
        return when {
            badgeItemType?.isTemplate == false -> {
                EXISTING_BADGE
            }
            badgeItemType?.isActive == true -> {
                TEMPLATE_BADGE
            }
            badgeItemType?.manageLabel == true -> {
                MANAGE_LABEL
            }
            else -> {
                MANAGE_LABEL
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when {
            viewType == EXISTING_BADGE -> {
                val binding = LayoutBadgesListItemHolderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ExistingBadgeViewHolder(binding)
            }
            viewType == TEMPLATE_BADGE -> {
                val binding = LayoutBadgesListItemHolderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return TemplateBadgeViewHolder(binding)
            }
            else -> {
                val binding = LayoutManageBadgesLabelItemHolderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ManageBadgeViewHolder(binding)
            }
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            EXISTING_BADGE == getItemViewType(position) -> {
                (holder as ExistingBadgeViewHolder).bind(position)
            }
            TEMPLATE_BADGE == getItemViewType(position) -> {
                (holder as TemplateBadgeViewHolder).bind(position)
            }
        }
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_tribe)
            .build()
    }

    inner class ExistingBadgeViewHolder(
        private val binding: LayoutBadgesListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)
        fun bind(position: Int) {
            binding.apply {
                val tribeBadge: TribeBadge? = tribeBadgesList.getOrNull(position)

                tribeBadge?.imageUrl?.let { imageUrl ->
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

                textViewBadgeTitle.text = tribeBadge?.name ?: ""
                textViewBadgeDescription.text = tribeBadge?.description ?: ""
                textViewBadgesRowCount.text = tribeBadge?.amount_issued.toString()
                textViewBadgesLeft.text = (tribeBadge?.amount_created?.minus(tribeBadge.amount_issued ?: 0)).toString()


                if (tribeBadge?.isActive == true) {
                    layoutButtonTemplate.textViewButtonSmall.text = getString(R.string.badges_active)
                    layoutButtonTemplate.layoutConstraintButtonSmall.background =
                        ContextCompat.getDrawable(root.context, R.drawable.background_button_open_white)
                    layoutButtonTemplate.textViewButtonSmall.setTextColor(ContextCompat.getColor(root.context, R.color.headerBG))
                }
                else {
                    layoutButtonTemplate.textViewButtonSmall.text = getString(R.string.badges_inactive)
                    layoutButtonTemplate.layoutConstraintButtonSmall.background =
                        ContextCompat.getDrawable(root.context, R.drawable.background_button_open)
                    layoutButtonTemplate.textViewButtonSmall.setTextColor(ContextCompat.getColor(root.context, R.color.secondaryText))
                }
            }
        }

        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }
    }

    inner class TemplateBadgeViewHolder(
        private val binding: LayoutBadgesListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private val disposables: ArrayList<Disposable> = ArrayList(2)
        fun bind(position: Int) {
            binding.apply {
                val tribeBadge: TribeBadge? = tribeBadgesList.getOrNull(position)

                tribeBadge?.imageUrl?.let { imageUrl ->
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
                val description = getString(R.string.badges_template_description)
                val earnOrSpend = tribeBadge?.rewardType?.let { getString(it) } ?: ""

                textViewBadgeTitle.text = tribeBadge?.name ?: ""
                textViewBadgeDescription.text = String.format(description, earnOrSpend, tribeBadge?.rewardRequirement)
                layoutButtonTemplate.textViewButtonSmall.text = getString(R.string.badges_template)
                layoutButtonTemplate.layoutConstraintButtonSmall.background = ContextCompat.getDrawable(root.context, R.drawable.background_button_join)
                textViewBadgesRowCount.invisible
                textViewBadgesLeft.invisible
            }
        }

        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }
    }

    inner class ManageBadgeViewHolder(
        private val binding: LayoutManageBadgesLabelItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        init {
            binding.layoutConstraintManageBadgesHolder.setOnClickListener {
                viewModel.goToCreateBadgeScreen("testing navigation")
            }
        }

    }
}
















