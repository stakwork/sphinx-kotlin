package chat.sphinx.tribe_members_list.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.tribe_members_list.R
import chat.sphinx.tribe_members_list.databinding.LayoutTribeMemberHolderBinding
import chat.sphinx.tribe_members_list.ui.TribeMembersListViewModel
import chat.sphinx.tribe_members_list.ui.TribeMembersListViewState
import chat.sphinx.tribe_members_list.ui.viewstate.TribeMemberHolderViewState
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_message.MessageType
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
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
                        old.contactDto?.id == new.contactDto?.id
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

    fun getPendingTribeMembersCount(): Int {
        return tribeMembers.count { tribeMember -> tribeMember.contactDto?.pending == true }
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

        private val holderJobs: ArrayList<Job> = ArrayList(6)
        private var disposable: Disposable? = null
        private var tribeMemberHolderViewState: TribeMemberHolderViewState? = null

        fun bind(position: Int) {
            tribeMemberHolderViewState = tribeMembers.getOrNull(position) ?: let {
                tribeMemberHolderViewState = null
                return
            }
            disposable?.dispose()

            tribeMemberHolderViewState?.apply {
                when (this) {
                    is TribeMemberHolderViewState.Loader -> { }
                    is TribeMemberHolderViewState.Member -> {
                        if (contactDto != null) {
                            bindContactDetails(binding, contactDto)
                        }
                    }
                    is TribeMemberHolderViewState.Pending -> {
                        if (contactDto != null) {
                            bindContactDetails(binding, contactDto)
                            bindAdminFunctions(binding, contactDto, position)
                        }
                    }
                    is TribeMemberHolderViewState.PendingTribeMemberHeader -> {
                        bindHeader(binding, "PENDING TRIBE MEMBERS")

                    }
                    is TribeMemberHolderViewState.TribeMemberHeader -> {
                        bindHeader(binding, "TRIBE MEMBERS")
                    }
                }
            }
        }

        private fun bindContactDetails(binding: LayoutTribeMemberHolderBinding, contactDto: ContactDto) {
            binding.apply {
                layoutConstraintTribeMemberContainer.visible
                layoutConstraintTribeMemberHeaderContainer.gone
                constraintLayoutTribeMemberRequestActions.gone

                textViewMemberName.text = contactDto.alias
                textViewNameGroup.text = contactDto.alias?.first().toString()

                if (contactDto.photo_url?.isNotEmpty() == true) {
                    onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                        imageLoader.load(
                            imageViewMemberPicture,
                            contactDto.photo_url!!,
                            imageLoaderOptions
                        )
                    }
                } else {
                    imageViewMemberPicture.setImageDrawable(
                        AppCompatResources.getDrawable(
                            binding.root.context,
                            R.drawable.ic_profile_avatar_circle
                        )
                    )
                }
            }
        }

        private fun bindAdminFunctions(binding: LayoutTribeMemberHolderBinding, contactDto: ContactDto, position: Int) {
            binding.apply {
                constraintLayoutTribeMemberRequestActions.visible

                textViewTribeMemberRequestAcceptAction.setOnClickListener {
                    processMembershipRequest(
                        layoutConstraintGroupActionJoinRequestProgressBarContainer,
                        ContactId(contactDto.id),
                        MessageType.GroupAction.MemberApprove,
                        position
                    )
                }
                textViewTribeMemberRequestRejectAction.setOnClickListener {
                    processMembershipRequest(
                        layoutConstraintGroupActionJoinRequestProgressBarContainer,
                        ContactId(contactDto.id),
                        MessageType.GroupAction.MemberReject,
                        position
                    )
                }
            }
        }

        private fun processMembershipRequest(
            layoutConstraintGroupActionJoinRequestProgressBarContainer : ConstraintLayout,
            contactId: ContactId,
            type: MessageType.GroupAction,
            position: Int
        ) {
            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                layoutConstraintGroupActionJoinRequestProgressBarContainer.visible

                when (viewModel.processMemberRequest(contactId, type)) {
                    LoadResponse.Loading -> { }
                    is Response.Error -> {
                        layoutConstraintGroupActionJoinRequestProgressBarContainer.gone
                        viewModel.showFailedToProcessMemberMessage(type)
                    }
                    is Response.Success -> {
                        tribeMembers.removeAt(position)
                        this@TribeMembersListAdapter.notifyItemRemoved(position)
                    }
                }
            }.let { job ->
                holderJobs.add(job)
            }
        }

        private fun bindHeader(binding: LayoutTribeMemberHolderBinding, headerText: String) {
            binding.apply {
                layoutConstraintTribeMemberContainer.gone
                layoutConstraintTribeMemberHeaderContainer.visible

                textViewTribeMemberHeader.text = headerText
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}