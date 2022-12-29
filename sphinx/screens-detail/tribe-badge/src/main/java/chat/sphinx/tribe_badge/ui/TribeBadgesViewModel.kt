package chat.sphinx.tribe_badge.ui

import android.content.Context
import chat.sphinx.tribe_badge.adapter.TribeBadgesNavigator
import chat.sphinx.tribe_badge.ui.TribeBadgesViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class TribeBadgesViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: TribeBadgesNavigator
): SideEffectViewModel<
        Context,
        TribeBadgesSideEffect,
        TribeBadgesViewState>(dispatchers, TribeBadgesViewState.Idle)
{


}
