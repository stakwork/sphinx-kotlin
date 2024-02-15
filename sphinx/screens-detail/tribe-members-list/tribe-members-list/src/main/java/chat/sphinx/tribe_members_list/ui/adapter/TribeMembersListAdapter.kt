package chat.sphinx.tribe_members_list.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.tribe_members_list.R
import chat.sphinx.tribe_members_list.databinding.LayoutTribeMemberHolderBinding
import chat.sphinx.tribe_members_list.ui.TribeMembersListViewModel
import chat.sphinx.tribe_members_list.ui.TribeMembersListViewState
import chat.sphinx.tribe_members_list.ui.viewstate.TribeMemberHolderViewState
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_message.SenderAlias
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.ArrayList

internal class TribeMembersListAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: TribeMembersListViewModel
): RecyclerView.Adapter<TribeMembersListAdapter.TribeMemberViewHolder>(), DefaultLifecycleObserver {

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    private inner class Diff(
        private val oldList: List<TribeMemberHolderViewState>,
        private val newList: List<TribeMemberHolderViewState>,
    ): DiffUtil.Callback()
    {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        @Volatile
        var sameList: Boolean = oldListSize == newListSize
            private set

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val same: Boolean =  try {
                oldList[oldItemPosition].let { old ->
                    newList[newItemPosition].let { new ->
                        old.pubkey == new.pubkey
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                false
            }

            if (sameList) {
                sameList = same
            }

            return same
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val same: Boolean = try {
                oldList[oldItemPosition].toString() == newList[newItemPosition].toString()
            } catch (e: IndexOutOfBoundsException) {
                false
            }

            if (sameList) {
                sameList = same
            }

            return same
        }

    }

    private val tribeMembers = ArrayList<TribeMemberHolderViewState>(viewModel.currentViewState.list)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->
                if (viewState is TribeMembersListViewState.ListMode) {
                    if (viewState.firstPage) {
                        tribeMembers.clear()
                    }

                    if (!viewState.loading && viewState.list.isNotEmpty()) {
                        if (tribeMembers.isEmpty()) {
                            tribeMembers.addAll(viewState.list)
                        } else {
                            val diff = Diff(tribeMembers, viewState.list)

                            withContext(viewModel.dispatchers.default) {
                                DiffUtil.calculateDiff(diff)
                            }.let {
                                if (!diff.sameList) {
                                    tribeMembers.removeLast()
                                    tribeMembers.addAll(viewState.list)
                                }
                            }
                        }
                        this@TribeMembersListAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return tribeMembers.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TribeMemberViewHolder {
        val binding = LayoutTribeMemberHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TribeMemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TribeMemberViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class TribeMemberViewHolder(
        private val binding: LayoutTribeMemberHolderBinding
    ): RecyclerView.ViewHolder(binding.root) {

        private val holderJobs: ArrayList<Job> = ArrayList(2)
        private var disposable: Disposable? = null
        private var tribeMemberHolderViewState: TribeMemberHolderViewState? = null

        fun bind(position: Int) {
            tribeMemberHolderViewState = tribeMembers.getOrNull(position) ?: let {
                tribeMemberHolderViewState = null
                return
            }

            for (job in holderJobs) {
                job.cancel()
            }
            holderJobs.clear()

            disposable?.dispose()

            tribeMemberHolderViewState?.apply {
                when (this) {
                    is TribeMemberHolderViewState.Loader -> {
                        bindLoader(binding)
                    }
                    is TribeMemberHolderViewState.Member -> {
                        bindContactDetails(binding, alias, photo_url, showInitial)
                    }
                    is TribeMemberHolderViewState.Pending -> {
                        bindContactDetails(binding, alias, photo_url, showInitial)

                        alias?.let { nnAlias ->
                            bindAdminFunctions(binding, alias, position)
                        }
                    }
                    is TribeMemberHolderViewState.PendingTribeMemberHeader -> {
                        bindHeader(binding, binding.root.context.getString(R.string.pending_tribe_members_header))
                    }
                    is TribeMemberHolderViewState.TribeMemberHeader -> {
                        bindHeader(binding, binding.root.context.getString(R.string.tribe_members_list_header))
                    }
                }
            }

            binding.layoutConstraintDeleteMember.setOnClickListener {
                removeAt(position)
            }
        }

        private fun bindLoader(binding: LayoutTribeMemberHolderBinding) {
            binding.includeLoadingMoreMembers.root.goneIfFalse(true)
        }

        private fun bindContactDetails(
            binding: LayoutTribeMemberHolderBinding,
            alias: String?,
            photoUrl: String?,
            shouldShowInitial: Boolean
        ) {
            binding.apply {
                layoutConstraintTribeMemberContainer.visible
                includeLoadingMoreMembers.root.gone
                layoutConstraintTribeMemberHeaderContainer.gone
                constraintLayoutTribeMemberRequestActions.gone

                textViewMemberName.text = alias ?: ""

                textViewMemberFirstInitial.text = alias?.firstOrNull()?.toString() ?: ""
                textViewMemberFirstInitial.goneIfFalse(shouldShowInitial)

                textViewMemberInitials.text = alias?.getInitials() ?: ""
                textViewMemberInitials.setBackgroundRandomColor(chat.sphinx.resources.R.drawable.chat_initials_circle)

                if (!photoUrl.isNullOrEmpty()) {
                    imageViewMemberPicture.visible

                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewMemberPicture,
                            photoUrl,
                            imageLoaderOptions
                        ).also {
                            disposable = it
                        }
                    }.let { job ->
                        holderJobs.add(job)
                    }
                } else {
                    imageViewMemberPicture.gone
                }
            }
        }

        private fun bindAdminFunctions(binding: LayoutTribeMemberHolderBinding, alias: String, position: Int) {
            binding.apply {
                constraintLayoutTribeMemberRequestActions.visible

                textViewTribeMemberRequestAcceptAction.setOnClickListener {
                    processMembershipRequest(
                        layoutConstraintGroupActionJoinRequestProgressBarContainer,
                        SenderAlias(alias),
                        MessageType.GroupAction.MemberApprove,
                        position
                    )
                }
                textViewTribeMemberRequestRejectAction.setOnClickListener {
                    processMembershipRequest(
                        layoutConstraintGroupActionJoinRequestProgressBarContainer,
                        SenderAlias(alias),
                        MessageType.GroupAction.MemberReject,
                        position
                    )
                }
            }
        }

        private fun processMembershipRequest(
            layoutConstraintGroupActionJoinRequestProgressBarContainer : ConstraintLayout,
            alias: SenderAlias,
            type: MessageType.GroupAction,
            position: Int
        ) {
            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                layoutConstraintGroupActionJoinRequestProgressBarContainer.visible

                viewModel.processMemberRequest(alias, type)
//                when (viewModel.processMemberRequest(alias, type)) {
//                    LoadResponse.Loading -> { }
//                    is Response.Error -> {
//                        layoutConstraintGroupActionJoinRequestProgressBarContainer.gone
//                        viewModel.showFailedToProcessMemberMessage(type)
//                    }
//                    is Response.Success -> {}
//                }
            }.let { job ->
                holderJobs.add(job)
            }
        }

        private fun bindHeader(binding: LayoutTribeMemberHolderBinding, headerText: String) {
            binding.apply {
                includeLoadingMoreMembers.root.gone
                layoutConstraintTribeMemberContainer.gone
                layoutConstraintTribeMemberHeaderContainer.visible

                textViewTribeMemberHeader.text = headerText
            }
        }
    }

    fun removeAt(position: Int) {
        val tribeMember = tribeMembers.elementAtOrNull(position)

        tribeMember?.pubkey?.toLightningNodePubKey()?.let {
            viewModel.kickMemberFromTribe(it)
            notifyItemRemoved(position)
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}