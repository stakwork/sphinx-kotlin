package chat.sphinx.dashboard.ui.feed.listen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedListenBinding
import chat.sphinx.dashboard.ui.DashboardFragment
import chat.sphinx.dashboard.ui.adapter.FeedListenNowAdapter
import chat.sphinx.dashboard.ui.adapter.FeedFollowingAdapter
import chat.sphinx.dashboard.ui.adapter.FeedRecentlyPlayedAdapter
import chat.sphinx.dashboard.ui.feed.FeedFragment
import chat.sphinx.dashboard.ui.placeholder.PlaceholderContent
import chat.sphinx.dashboard.ui.viewstates.FeedListenViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class FeedListenFragment : SideEffectFragment<
        Context,
        FeedListenSideEffect,
        FeedListenViewState,
        FeedListenViewModel,
        FragmentFeedListenBinding
        >(R.layout.fragment_feed_listen)
{

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: FeedListenViewModel by viewModels()
    override val binding: FragmentFeedListenBinding by viewBinding(FragmentFeedListenBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNestedScrollView()
        setupRecentlyReleaseAdapter()
        setupRecentlyPlayedAdapter()
    }

    @SuppressLint("RestrictedApi")
    private fun setupNestedScrollView() {
        binding.scrollViewContent.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (parentFragment is FeedFragment) {
                val bottomOfScroll = !binding.scrollViewContent.canScrollVertically(1)
                val topOfScroll = !binding.scrollViewContent.canScrollVertically(-1)
                val scrollNotAvailable = (bottomOfScroll && topOfScroll)

                (parentFragment as FeedFragment)?.shouldToggleNavBar(
                    (scrollY <= oldScrollY && !bottomOfScroll) || scrollNotAvailable
                )
            }
        }
    }

    private fun setupRecentlyReleaseAdapter() {
        binding.recyclerViewListenNow.apply {
            val listenNowAdapter = FeedListenNowAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )

            this.setHasFixedSize(false)
            adapter = listenNowAdapter
            itemAnimator = null
        }
    }

    private fun setupRecentlyPlayedAdapter() {
        binding.recyclerViewRecentlyPlayed.apply {
            val recentlyPlayedAdapter = FeedRecentlyPlayedAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                viewModel
            )

            this.setHasFixedSize(false)
            adapter = recentlyPlayedAdapter
            itemAnimator = null
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: FeedListenSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    companion object {
        fun newInstance(): FeedListenFragment {
            return FeedListenFragment()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: FeedListenViewState) {
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

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.lastPlayedFeedsHolderViewStateFlow.collect { list ->
                if (list.isEmpty()) {
                    binding.layoutConstraintRecentlyPlayed.gone
                } else {
                    binding.layoutConstraintRecentlyPlayed.visible
                }
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
