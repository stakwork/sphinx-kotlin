package chat.sphinx.dashboard.ui.feed.all

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedAllBinding
import chat.sphinx.dashboard.ui.adapter.FeedDownloadedAdapter
import chat.sphinx.dashboard.ui.adapter.FeedFollowingAdapter
import chat.sphinx.dashboard.ui.adapter.FeedRecentlyPlayedAdapter
import chat.sphinx.dashboard.ui.adapter.FeedRecommendationsAdapter
import chat.sphinx.dashboard.ui.feed.FeedFragment
import chat.sphinx.dashboard.ui.viewstates.FeedAllViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class FeedAllFragment : SideEffectFragment<
        FragmentActivity,
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

        setupRecommendationsAdapter()
        setupFollowingAdapter()
        setupRecentlyPlayedAdapter()
        setupDownloadedAdapter()
        setupRefreshButton()
        setupNestedScrollView()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFeedRecommendations()
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

    private fun setupRecommendationsAdapter() {
        binding.recyclerViewRecommendations.apply {
            val listenNowAdapter = FeedRecommendationsAdapter(
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

    private fun setupDownloadedAdapter() {
        binding.recyclerViewDownloaded.apply {
            val downloadedAdapter = FeedDownloadedAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                viewModel
            )
            this.setHasFixedSize(false)
            adapter = downloadedAdapter
            itemAnimator = null
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: FeedAllSideEffect) {
        sideEffect.execute(requireActivity())
    }

    companion object {
        fun newInstance(): FeedAllFragment {
            return FeedAllFragment()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: FeedAllViewState) {
        binding.apply {
            when(viewState){
                is FeedAllViewState.Loading -> {
                    layoutConstraintLoading.visible
                    layoutConstraintRefresh.gone
                    recyclerViewRecommendations.invisible
                    layoutConstraintNoRecommendations.gone
                }
                is FeedAllViewState.NoRecommendations -> {
                    layoutConstraintLoading.gone
                    layoutConstraintRefresh.visible
                    recyclerViewRecommendations.invisible
                    layoutConstraintNoRecommendations.visible
                }
                is FeedAllViewState.RecommendedList -> {
                    layoutConstraintLoading.gone
                    layoutConstraintRefresh.visible
                    recyclerViewRecommendations.visible
                    layoutConstraintNoRecommendations.gone
                }
                is FeedAllViewState.Disabled -> {
                    textViewListenRecommendationsHeader.gone
                    textView2.gone
                    layoutConstraintRefresh.gone
                    refreshButtonIcon.gone
                    layoutConstraintLoading.gone
                    layoutConstraintRefresh.gone
                    recyclerViewRecommendations.gone
                    layoutConstraintNoRecommendations.gone
                }
            }
        }
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
            viewModel.feedDownloadedHolderViewStateFlow.collect { list ->
                if (list.isEmpty()) {
                    binding.layoutConstraintDownloadedSection.gone
                } else {
                    binding.layoutConstraintDownloadedSection.visible
                }
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
            layoutConstraintFollowing.goneIfFalse(contentAvailable)
        }
    }

    private fun setupRefreshButton() {
        binding.apply {
            layoutConstraintRefresh.setOnClickListener {
                viewModel.loadFeedRecommendations()
            }
        }
    }

}
