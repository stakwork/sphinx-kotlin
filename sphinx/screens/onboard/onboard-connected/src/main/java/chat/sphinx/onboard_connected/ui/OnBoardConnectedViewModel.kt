package chat.sphinx.onboard_connected.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.onboard_connected.navigation.OnBoardConnectedNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class OnBoardConnectedViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardConnectedNavigator,
): BaseViewModel<
        OnBoardConnectedViewState
        >(dispatchers, OnBoardConnectedViewState.Idle)
{

    fun continueToDashboardScreen() {
        viewModelScope.launch(mainImmediate) {
            navigator.toDashboardScreen(updateBackgroundLoginTime = true)
        }
    }
}