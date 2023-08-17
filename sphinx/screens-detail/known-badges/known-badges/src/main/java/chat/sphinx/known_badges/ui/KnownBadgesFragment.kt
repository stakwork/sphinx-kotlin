package chat.sphinx.known_badges.ui

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
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.known_badges.R
import chat.sphinx.known_badges.adapter.KnownBadgesListAdapter
import chat.sphinx.known_badges.adapter.KnownBadgesListFooterAdapter
import chat.sphinx.known_badges.databinding.FragmentKnownBadgesBinding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class KnownBadgesFragment: SideEffectDetailFragment<
        Context,
        KnownBadgesSideEffect,
        KnownBadgesViewState,
        KnownBadgesViewModel,
        FragmentKnownBadgesBinding
        >(R.layout.fragment_known_badges)
{
    override val binding: FragmentKnownBadgesBinding by viewBinding(FragmentKnownBadgesBinding::bind)
    override val viewModel: KnownBadgesViewModel by viewModels()

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.layoutConstraintKnownBadgesFragment)

        binding.includeKnownBadgesHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.known_badges_title)

            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupKnownBadgesListAdapter()
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    private fun setupKnownBadgesListAdapter(){
        binding.recyclerViewList.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val knownBadgesListAdapter = KnownBadgesListAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )
            val knownBadgesFooterAdapter = KnownBadgesListFooterAdapter(requireActivity() as InsetterActivity)
            this.setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(knownBadgesListAdapter, knownBadgesFooterAdapter)
            layoutManager = linearLayoutManager
            itemAnimator = null
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: KnownBadgesSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: KnownBadgesViewState) {
        when (viewState) {
            is KnownBadgesViewState.Idle -> {}
            is KnownBadgesViewState.Loading -> {
                binding.apply {
                    progressBarKnownBadges.visible
                    recyclerViewList.gone
                    textViewNoFoundBadges.gone
                }
            }
            is KnownBadgesViewState.KnownBadges -> {
                binding.apply {
                    progressBarKnownBadges.gone
                    recyclerViewList.goneIfFalse(viewState.badges.isNotEmpty())
                    textViewNoFoundBadges.goneIfFalse(viewState.badges.isEmpty())
                }
            }
        }
    }
}
