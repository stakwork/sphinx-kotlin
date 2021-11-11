package chat.sphinx.dashboard.ui.feed.listen

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedListenBinding
import chat.sphinx.dashboard.ui.adapter.FeedListenEpisodeAdapter
import chat.sphinx.dashboard.ui.adapter.FeedListenPodcastAdapter
import chat.sphinx.dashboard.ui.placeholder.PlaceholderContent
import chat.sphinx.dashboard.ui.viewstates.FeedListenViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment

@AndroidEntryPoint
internal class FeedListenFragment : SideEffectFragment<
        Context,
        FeedListenSideEffect,
        FeedListenViewState,
        FeedListenViewModel,
        FragmentFeedListenBinding
        >(R.layout.fragment_feed_listen)
{
    override val viewModel: FeedListenViewModel by viewModels()
    override val binding: FragmentFeedListenBinding by viewBinding(FragmentFeedListenBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFeedListenEpisodeRecyclerView()
    }

    private fun setupFeedListenEpisodeRecyclerView() {
        binding.apply {
            with(recyclerViewListenNowEpisodes) {
                adapter = FeedListenEpisodeAdapter(PlaceholderContent.ITEMS)
            }
            with(recyclerViewListenNowPodcasts) {
                adapter = FeedListenPodcastAdapter(PlaceholderContent.ITEMS)
            }
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
}
