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
import chat.sphinx.tribe_badge.R
import chat.sphinx.tribe_badge.adapter.TribeBadgesListAdapter
import chat.sphinx.tribe_badge.adapter.TribeBadgesListFooterAdapter
import chat.sphinx.tribe_badge.databinding.FragmentTribeBadgesBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class TribeBadgesFragment: SideEffectFragment<
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

    override suspend fun onViewStateFlowCollect(viewState: TribeBadgesViewState) {
        @Exhaustive
        when (viewState) {
            is TribeBadgesViewState.Idle -> {}
            is TribeBadgesViewState.Loading -> {
                binding.layoutConstraintProgressBarContainer.visible
            }
            is TribeBadgesViewState.Close -> {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
            is TribeBadgesViewState.TribeBadgesList -> {
                binding.layoutConstraintProgressBarContainer.gone
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()
    }

    override suspend fun onSideEffectCollect(sideEffect: TribeBadgesSideEffect) {
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
            val TribeBadgesFooterAdapter =
                TribeBadgesListFooterAdapter(requireActivity() as InsetterActivity)
            this.setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(tribeBadgesListAdapter, TribeBadgesFooterAdapter)
            layoutManager = linearLayoutManager
            itemAnimator = null
        }
    }
    private fun setupHeaderScreen() {
        binding.includeLayoutKnownBadgesTitle.apply {
            textViewDetailScreenHeaderNavBack.visible
            textViewDetailScreenHeaderName.text = getString(R.string.badges_header)
            textViewDetailScreenClose.gone
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