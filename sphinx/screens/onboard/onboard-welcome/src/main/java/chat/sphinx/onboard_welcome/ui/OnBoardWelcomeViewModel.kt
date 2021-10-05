package chat.sphinx.onboard_welcome.ui

import android.content.Context
import chat.sphinx.onboard_welcome.navigation.OnBoardWelcomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class OnBoardWelcomeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardWelcomeNavigator,
): SideEffectViewModel<
        Context,
        OnBoardWelcomeSideEffect,
        OnBoardWelcomeViewState
        >(dispatchers, OnBoardWelcomeViewState.Idle)
{
    fun goToNewUserScreen() {

    }

    fun goToExistingUserScreen() {

    }
}
