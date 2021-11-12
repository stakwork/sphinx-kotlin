package chat.sphinx.dashboard.ui.feed.read

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedAllBinding
import chat.sphinx.dashboard.databinding.FragmentFeedReadBinding
import chat.sphinx.dashboard.ui.viewstates.FeedReadViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment

@AndroidEntryPoint
internal class FeedReadFragment : SideEffectFragment<
        Context,
        FeedReadSideEffect,
        FeedReadViewState,
        FeedReadViewModel,
        FragmentFeedReadBinding
        >(R.layout.fragment_feed_read)
{
    override val viewModel: FeedReadViewModel by viewModels()
    override val binding: FragmentFeedReadBinding by viewBinding(FragmentFeedReadBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
}
