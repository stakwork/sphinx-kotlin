package chat.sphinx.authentication.ui

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.sphinx.authentication.components.SphinxAuthenticationViewCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import io.matthewnelson.feature_authentication_view.ui.AuthenticationEventHandler
import io.matthewnelson.feature_authentication_view.ui.AuthenticationViewModelContainer
import io.matthewnelson.feature_authentication_view.ui.AuthenticationViewState
import javax.inject.Inject

@HiltViewModel
internal class AuthenticationViewModel @Inject constructor(
    authenticationManager: AuthenticationCoreManager,
    dispatchers: CoroutineDispatchers,
    sphinxAuthenticationViewCoordinator: SphinxAuthenticationViewCoordinator
): SideEffectViewModel<
        FragmentActivity,
        AuthenticationSideEffect,
        AuthenticationViewState
        >(dispatchers, AuthenticationViewState.Idle())
{
    override val viewStateContainer: ViewStateContainer<AuthenticationViewState>
        get() = authenticationViewModelContainer.viewStateContainer

    @Suppress("RemoveExplicitTypeArguments")
    val authenticationViewModelContainer: AuthenticationViewModelContainer<NavController> by lazy {
        AuthenticationViewModelContainer<NavController>(
            authenticationManager,
            dispatchers,
            SphinxAuthenticationEventHandler(),
            sphinxAuthenticationViewCoordinator,
            shufflePinNumbers = false,
            viewModelScope = viewModelScope
        )
    }

    private inner class SphinxAuthenticationEventHandler: AuthenticationEventHandler() {
        override suspend fun onNewPinDoesNotMatchConfirmedPin() {
            submitSideEffect(AuthenticationSideEffect.Notify.PinDoesNotMatch)
        }

        override suspend fun onOneMoreAttemptUntilLockout() {
            submitSideEffect(AuthenticationSideEffect.Notify.OneMoreAttemptBeforeLockout)
        }

        override suspend fun onPinDoesNotMatch() {
            submitSideEffect(AuthenticationSideEffect.Notify.PinDoesNotMatch)
        }

        override suspend fun onWrongPin() {
            submitSideEffect(AuthenticationSideEffect.Notify.WrongPin)
        }

        override suspend fun produceHapticFeedback() {
            submitSideEffect(AuthenticationSideEffect.ProduceHapticFeedback)
        }
    }
}
