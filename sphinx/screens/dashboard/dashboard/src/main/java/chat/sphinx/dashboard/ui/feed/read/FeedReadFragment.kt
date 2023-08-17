package chat.sphinx.dashboard.ui.feed.read

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedReadBinding
import chat.sphinx.dashboard.ui.adapter.FeedFollowingAdapter
import chat.sphinx.dashboard.ui.adapter.FeedReadNowAdapter
import chat.sphinx.dashboard.ui.feed.FeedFragment
import chat.sphinx.dashboard.ui.viewstates.FeedReadViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class FeedReadFragment : SideEffectFragment<
        Context,
        FeedReadSideEffect,
        FeedReadViewState,
        FeedReadViewModel,
        FragmentFeedReadBinding
        >(R.layout.fragment_feed_read)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: FeedReadViewModel by viewModels()
    override val binding: FragmentFeedReadBinding by viewBinding(FragmentFeedReadBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNestedScrollView()
        setupReadNowAdapter()
        setupFollowingAdapter()
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

    private fun setupReadNowAdapter() {
        binding.recyclerViewReadNow.apply {
            val readNowAdapter = FeedReadNowAdapter(
                this,
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )

            this.setHasFixedSize(false)
            adapter = readNowAdapter
            itemAnimator = null
        }
    }

    private fun setupFollowingAdapter() {
        binding.recyclerViewFollowing.apply {
            val followingAdapter = FeedFollowingAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                viewModel
            )

            this.setHasFixedSize(false)
            adapter = followingAdapter
            itemAnimator = null
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: FeedReadSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    companion object {
        fun newInstance(): FeedReadFragment {
            return FeedReadFragment()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: FeedReadViewState) {
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
