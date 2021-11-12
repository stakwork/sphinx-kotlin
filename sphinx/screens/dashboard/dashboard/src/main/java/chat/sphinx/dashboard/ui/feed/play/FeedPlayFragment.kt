package chat.sphinx.dashboard.ui.feed.play

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedAllBinding
import chat.sphinx.dashboard.databinding.FragmentFeedPlayBinding
import chat.sphinx.dashboard.ui.viewstates.FeedAllViewState
import chat.sphinx.dashboard.ui.viewstates.FeedPlayViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment

@AndroidEntryPoint
internal class FeedPlayFragment : SideEffectFragment<
        Context,
        FeedPlaySideEffect,
        FeedPlayViewState,
        FeedPlayViewModel,
        FragmentFeedPlayBinding
        >(R.layout.fragment_feed_play)
{
    override val viewModel: FeedPlayViewModel by viewModels()
    override val binding: FragmentFeedPlayBinding by viewBinding(FragmentFeedPlayBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override suspend fun onSideEffectCollect(sideEffect: FeedPlaySideEffect) {
        sideEffect.execute(binding.root.context)
    }

    companion object {
        fun newInstance(): FeedPlayFragment {
            return FeedPlayFragment()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: FeedPlayViewState) {
        // TODO("Not yet implemented")
    }
}
