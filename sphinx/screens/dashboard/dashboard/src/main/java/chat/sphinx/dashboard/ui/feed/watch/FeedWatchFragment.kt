package chat.sphinx.dashboard.ui.feed.watch

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedWatchBinding
import chat.sphinx.dashboard.ui.viewstates.FeedWatchViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment

@AndroidEntryPoint
internal class FeedWatchFragment : SideEffectFragment<
        Context,
        FeedWatchSideEffect,
        FeedWatchViewState,
        FeedWatchViewModel,
        FragmentFeedWatchBinding
        >(R.layout.fragment_feed_watch)
{
    override val viewModel: FeedWatchViewModel by viewModels()
    override val binding: FragmentFeedWatchBinding by viewBinding(FragmentFeedWatchBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
}
