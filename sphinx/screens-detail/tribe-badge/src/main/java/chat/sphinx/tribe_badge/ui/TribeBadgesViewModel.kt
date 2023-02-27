package chat.sphinx.tribe_badge.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.tribe_badge.navigation.TribeBadgesNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
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
    fun goToCreateBadgeScreen(badgeName: String) {
        viewModelScope.launch(mainImmediate) {
            navigator.toCreateBadgeScreen(badgeName)
        }
    }


}
