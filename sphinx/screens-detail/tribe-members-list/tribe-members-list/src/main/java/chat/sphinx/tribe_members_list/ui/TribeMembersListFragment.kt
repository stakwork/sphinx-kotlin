package chat.sphinx.tribe_members_list.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.tribe_members_list.R
import chat.sphinx.tribe_members_list.databinding.FragmentTribeMembersListBinding
import chat.sphinx.tribe_members_list.ui.adapter.TribeMembersListAdapter
import chat.sphinx.tribe_members_list.ui.adapter.TribeMembersListFooterAdapter
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class TribeMembersListFragment: SideEffectDetailFragment<
        Context,
        TribeMembersListSideEffect,
        TribeMembersListViewState,
        TribeMembersListViewModel,
        FragmentTribeMembersListBinding
        >(R.layout.fragment_tribe_members_list)
{
    override val viewModel: TribeMembersListViewModel by viewModels()
    override val binding: FragmentTribeMembersListBinding by viewBinding(FragmentTribeMembersListBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader<ImageView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.includeTribeMembersListHeader.apply {
            textViewDetailScreenHeaderNavBack.visible
            textViewDetailScreenHeaderName.text = getString(R.string.tribe_members_list_header)

            textViewDetailScreenClose.gone

            textViewDetailScreenHeaderNavBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
        }

        setupTribeMembers()
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    private fun setupTribeMembers() {
        val tribeMembersListAdapter = TribeMembersListAdapter(
            imageLoader, viewLifecycleOwner, onStopSupervisor, viewModel)
        val tribeMembersListFooterAdapter = TribeMembersListFooterAdapter(requireActivity() as InsetterActivity)

        binding.recyclerViewTribeMembersList.apply {
            this.setHasFixedSize(false)
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = ConcatAdapter(tribeMembersListAdapter, tribeMembersListFooterAdapter)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        lifecycleScope.launch(viewModel.mainImmediate) {
                            viewModel.loadMoreTribeMembers()
                        }
                    }
                }
            })
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: TribeMembersListViewState) {
        if (viewState is TribeMembersListViewState.ListMode) {
//            if (!viewState.firstPage) {
//                return
//            }

            binding.apply {
                progressBarTribeMembersList.goneIfFalse(
                    viewState.loading
                )

                textViewNoTribeMembersList.goneIfFalse(
                    !viewState.loading && viewState.list.isEmpty()
                )

                recyclerViewTribeMembersList.goneIfFalse(
                    !viewState.loading && viewState.list.isNotEmpty()
                )
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: TribeMembersListSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
