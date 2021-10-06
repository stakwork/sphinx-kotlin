package chat.sphinx.onboard_welcome.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.onboard_welcome.navigation.OnBoardWelcomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class OnBoardWelcomeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardWelcomeNavigator,
): BaseViewModel<
        OnBoardWelcomeViewState
        >(dispatchers, OnBoardWelcomeViewState.Idle)
{
    fun goToNewUserScreen() {
        viewModelScope.launch(mainImmediate) {
            navigator.toOnBoardDescriptionScreen(true)
        }
    }

    fun goToExistingUserScreen() {
        viewModelScope.launch(mainImmediate) {
            navigator.toOnBoardDescriptionScreen(false)
        }
    }
}
