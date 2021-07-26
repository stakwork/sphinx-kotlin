package chat.sphinx.onboard.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.onboard.navigation.OnBoardNavigator
import chat.sphinx.onboard_common.OnBoardStepHandler
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class OnBoardViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardNavigator,
    private val onBoardStepHandler: OnBoardStepHandler,
    private val relayDataHandler: RelayDataHandler,
    private val authenticationCoordinator: AuthenticationCoordinator
): SideEffectViewModel<
        Context,
        OnBoardSideEffect,
        OnBoardViewState
        >(dispatchers, OnBoardViewState.Idle)
{

    fun presentLoginModal(
        authToken: AuthorizationToken,
        relayUrl: RelayUrl,
        inviterData: OnBoardInviterData,
    ) {
        viewModelScope.launch(mainImmediate) {
            authenticationCoordinator.submitAuthenticationRequest(
                AuthenticationRequest.LogIn()
            ).firstOrNull().let { response ->
                @Exhaustive
                when (response) {
                    null,
                    is AuthenticationResponse.Failure -> {
                        // will not be returned as back press for handling
                        // a LogIn request minimizes the application until
                        // User has authenticated
                    }
                    is AuthenticationResponse.Success.Authenticated -> {

                        relayDataHandler.persistAuthorizationToken(authToken)
                        relayDataHandler.persistRelayUrl(relayUrl)

                        val step2 = onBoardStepHandler.persistOnBoardStep2Data(inviterData)

                        if (step2 != null) {
                            navigator.toOnBoardNameScreen(step2)
                        } else {
                            // TODO: Handle persistence error
                        }
                    }
                    is AuthenticationResponse.Success.Key -> {
                        // will never be returned
                    }
                }
            }
        }
    }
}
