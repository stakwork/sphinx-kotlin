package chat.sphinx.tribe_badge.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.tribe_badge.R
import chat.sphinx.tribe_badge.adapter.TribeBadgesListAdapter
import chat.sphinx.tribe_badge.adapter.TribeBadgesListFooterAdapter
import chat.sphinx.tribe_badge.databinding.FragmentTribeBadgesBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class TribeBadgesFragment: SideEffectDetailFragment<
        Context,
        TribeBadgesSideEffect,
        TribeBadgesViewState,
        TribeBadgesViewModel,
        FragmentTribeBadgesBinding
        >(R.layout.fragment_tribe_badges)
{
    override val viewModel: TribeBadgesViewModel by viewModels()
    override val binding: FragmentTribeBadgesBinding by viewBinding(FragmentTribeBadgesBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeaderScreen()
        setupTribeBadgesListAdapter()
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: TribeBadgesViewState) {
        @Exhaustive
        when (viewState) {
            is TribeBadgesViewState.Idle -> {}
            is TribeBadgesViewState.Loading -> {
                binding.layoutConstraintProgressBarContainer.visible
            }
            is TribeBadgesViewState.Error -> {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
            is TribeBadgesViewState.TribeBadgesList -> {
                binding.layoutConstraintProgressBarContainer.gone
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: TribeBadgesSideEffect) {
        sideEffect.execute(requireActivity())
    }

    private fun setupTribeBadgesListAdapter(){
        binding.recyclerViewList.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val tribeBadgesListAdapter = TribeBadgesListAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )
            val tribeBadgesFooterAdapter = TribeBadgesListFooterAdapter(requireActivity() as InsetterActivity)
            this.setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(tribeBadgesListAdapter, tribeBadgesFooterAdapter)
            layoutManager = linearLayoutManager
            itemAnimator = null
        }
    }

    private fun setupHeaderScreen() {
        binding.apply {
            textViewDetailScreenHeaderNavBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.getBadgesTemplates()
    }
}