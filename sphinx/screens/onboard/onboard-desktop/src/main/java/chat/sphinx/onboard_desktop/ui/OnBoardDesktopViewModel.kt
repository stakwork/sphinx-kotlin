package chat.sphinx.onboard_desktop.ui

import chat.sphinx.onboard_desktop.navigation.OnBoardDesktopNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class OnBoardDesktopViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardDesktopNavigator,
): BaseViewModel<
        OnBoardDesktopViewState
        >(dispatchers, OnBoardDesktopViewState.Idle)
{
}