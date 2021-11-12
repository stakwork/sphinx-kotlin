package chat.sphinx.dashboard.ui.feed.all

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedAllBinding
import chat.sphinx.dashboard.ui.adapter.FeedFollowingAdapter
import chat.sphinx.dashboard.ui.adapter.FeedListenNowAdapter
import chat.sphinx.dashboard.ui.feed.listen.FeedListenFragment
import chat.sphinx.dashboard.ui.feed.listen.FeedListenSideEffect
import chat.sphinx.dashboard.ui.viewstates.FeedAllViewState
import chat.sphinx.dashboard.ui.viewstates.FeedListenViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class FeedAllFragment : SideEffectFragment<
        Context,
        FeedAllSideEffect,
        FeedAllViewState,
        FeedAllViewModel,
        FragmentFeedAllBinding
        >(R.layout.fragment_feed_all)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: FeedAllViewModel by viewModels()
    override val binding: FragmentFeedAllBinding by viewBinding(FragmentFeedAllBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFollowingAdapter()
    }

    private fun setupFollowingAdapter() {
        binding.recyclerViewFollowing.apply {
            val listenNowAdapter = FeedFollowingAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                viewModel
            )

            this.setHasFixedSize(false)
            adapter = listenNowAdapter
            itemAnimator = null
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: FeedAllSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    companion object {
        fun newInstance(): FeedAllFragment {
            return FeedAllFragment()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: FeedAllViewState) {
        // TODO("Not yet implemented")
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.feedsHolderViewStateFlow.collect { list ->
                toggleElements(
                    list.isNotEmpty()
                )
            }
        }
    }

    private fun toggleElements(contentAvailable: Boolean) {
        binding.apply {
            scrollViewContent.goneIfFalse(contentAvailable)
            textViewPlaceholder.goneIfFalse(!contentAvailable)
        }
    }
}
