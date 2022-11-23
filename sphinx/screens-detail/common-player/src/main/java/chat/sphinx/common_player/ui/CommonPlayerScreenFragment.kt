package chat.sphinx.common_player.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.common_player.R
import chat.sphinx.common_player.adapter.RecommendedItemsAdapter
import chat.sphinx.common_player.adapter.RecommendedItemsFooterAdapter
import chat.sphinx.common_player.databinding.FragmentCommonPlayerScreenBinding
import chat.sphinx.common_player.viewstate.CommonPlayerScreenViewState
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class CommonPlayerScreenFragment() : SideEffectFragment<
        Context,
        CommonPlayerScreenSideEffect,
        CommonPlayerScreenViewState,
        CommonPlayerScreenViewModel,
        FragmentCommonPlayerScreenBinding
        >(R.layout.fragment_common_player_screen) {

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentCommonPlayerScreenBinding by viewBinding(
        FragmentCommonPlayerScreenBinding::bind
    )
    override val viewModel: CommonPlayerScreenViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
//            textViewDismissButton.setOnClickListener {
//                lifecycleScope.launch(viewModel.mainImmediate) {
//                    viewModel.navigator.closeDetailScreen()
//                }
//            }
        }

        setupItems()
    }

    override suspend fun onViewStateFlowCollect(viewState: CommonPlayerScreenViewState) {
        @Exhaustive
        when(viewState) {
            is CommonPlayerScreenViewState.Idle -> {
                print("test")
            }
            is CommonPlayerScreenViewState.FeedRecommendations -> {
                binding.apply {
                    includeLayoutPlayerDescriptionAndControls.apply {
                        textViewItemTitle.text = viewState.selectedItem.title
                        textViewItemDescription.text = viewState.selectedItem.description
                    }
                }

                when(viewState) {
                    is CommonPlayerScreenViewState.FeedRecommendations.PodcastSelected -> {

                    }
                    is CommonPlayerScreenViewState.FeedRecommendations.YouTubeVideoSelected -> {

                    }
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: CommonPlayerScreenSideEffect) {
    }

    private fun setupItems() {
        binding.includeRecommendedItemsList.recyclerViewList.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val recommendedItemsAdapter = RecommendedItemsAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
            )
            val recommendedListFooterAdapter =
                RecommendedItemsFooterAdapter(requireActivity() as InsetterActivity)
            this.setHasFixedSize(false)

            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(recommendedItemsAdapter, recommendedListFooterAdapter)
            itemAnimator = null
        }
    }

}