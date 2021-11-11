package chat.sphinx.dashboard.ui.feed.all

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedAllBinding
import chat.sphinx.dashboard.ui.viewstates.FeedAllViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment

@AndroidEntryPoint
internal class FeedAllFragment : SideEffectFragment<
        Context,
        FeedAllSideEffect,
        FeedAllViewState,
        FeedAllViewModel,
        FragmentFeedAllBinding
        >(R.layout.fragment_feed_all)
{
    override val viewModel: FeedAllViewModel by viewModels()
    override val binding: FragmentFeedAllBinding by viewBinding(FragmentFeedAllBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
}
