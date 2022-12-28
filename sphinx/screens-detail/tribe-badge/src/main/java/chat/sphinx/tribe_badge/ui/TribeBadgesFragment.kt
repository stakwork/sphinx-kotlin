package chat.sphinx.tribe_badge.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.tribe_badge.R
import chat.sphinx.tribe_badge.databinding.FragmentTribeBadgesBinding
import chat.sphinx.tribe_badge.ui.TribeBadgesViewModel
import chat.sphinx.tribe_badge.ui.TribeBadgesViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment

@AndroidEntryPoint
internal class TribeBadgesFragment: SideEffectFragment<
        Context,
        TribeBadgesSideEffect,
        TribeBadgesViewState,
        TribeBadgesViewModel,
        FragmentTribeBadgesBinding
        >(R.layout.fragment_tribe_badges)
{
    override val viewModel: TribeBadgesViewModel by viewModels()
    override val binding: FragmentTribeBadgesBinding by viewBinding(FragmentTribeBadgesBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override suspend fun onViewStateFlowCollect(viewState: TribeBadgesViewState) {

    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()
    }

    override suspend fun onSideEffectCollect(sideEffect: TribeBadgesSideEffect) {
    }
}