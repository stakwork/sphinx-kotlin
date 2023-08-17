package chat.sphinx.dashboard.ui.feed.watch

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedWatchBinding
import chat.sphinx.dashboard.ui.adapter.FeedFollowingAdapter
import chat.sphinx.dashboard.ui.adapter.FeedRecentlyPlayedAdapter
import chat.sphinx.dashboard.ui.adapter.FeedWatchNowAdapter
import chat.sphinx.dashboard.ui.feed.FeedFragment
import chat.sphinx.dashboard.ui.feed.FeedRecentlyPlayedViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedWatchViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class FeedWatchFragment : SideEffectFragment<
        Context,
        FeedWatchSideEffect,
        FeedWatchViewState,
        FeedWatchViewModel,
        FragmentFeedWatchBinding
        >(R.layout.fragment_feed_watch)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: FeedWatchViewModel by viewModels()
    override val binding: FragmentFeedWatchBinding by viewBinding(FragmentFeedWatchBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNestedScrollView()
        setupListenNowAdapter()
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

    private fun setupListenNowAdapter() {
        binding.recyclerViewWatchNow.apply {
            val watchNowAdapter = FeedWatchNowAdapter(
                this,
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )

            this.setHasFixedSize(false)
            adapter = watchNowAdapter
            itemAnimator = null
        }
    }

    private fun setupRecentlyPlayedAdapter() {
        binding.recyclerViewFollowing.apply {
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
    override suspend fun onSideEffectCollect(sideEffect: FeedWatchSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    companion object {
        fun newInstance(): FeedWatchFragment {
            return FeedWatchFragment()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: FeedWatchViewState) {
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
