package chat.sphinx.onboard_lightning.ui

import chat.sphinx.onboard_lightning.navigation.OnBoardLightningNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class OnBoardLightningViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardLightningNavigator,
): BaseViewModel<
        OnBoardLightningViewState
        >(dispatchers, OnBoardLightningViewState.Idle)
{
}