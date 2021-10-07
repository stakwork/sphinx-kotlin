package chat.sphinx.onboard.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.onboard.navigation.OnBoardNavigator
import chat.sphinx.onboard_common.OnBoardStepHandler
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.isOnionAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
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

    private var loginJob: Job? = null
    fun presentLoginModal(
        authToken: AuthorizationToken,
        relayUrl: RelayUrl,
        inviterData: OnBoardInviterData,
    ) {
        if (loginJob?.isActive == true || proceedJob?.isActive == true) {
            return
        }

        updateViewState(OnBoardViewState.Saving)

        loginJob = viewModelScope.launch(mainImmediate) {
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

                        if (relayUrl.value.startsWith("http://") && !relayUrl.isOnionAddress) {
                            submitSideEffect(
                                OnBoardSideEffect.RelayUrlHttpConfirmation(
                                    relayUrl = relayUrl,
                                    callback = { url ->
                                        if (url != null) {
                                            proceedToLightningScreen(authToken, inviterData, url)
                                        } else {
                                            // cancelled
                                            updateViewState(OnBoardViewState.Idle)
                                        }
                                    }
                                )
                            )
                        } else {
                            proceedToLightningScreen(authToken, inviterData, relayUrl)
                        }
                    }
                    is AuthenticationResponse.Success.Key -> {
                        // will never be returned
                    }
                }
            }
        }
    }

    private var proceedJob: Job? = null
    private fun proceedToLightningScreen(
        authorizationToken: AuthorizationToken,
        inviterData: OnBoardInviterData,
        relayUrl: RelayUrl,
    ) {
        if (proceedJob?.isActive == true) {
            return
        }

        proceedJob = viewModelScope.launch(mainImmediate) {
            relayDataHandler.persistAuthorizationToken(authorizationToken)
            relayDataHandler.persistRelayUrl(relayUrl)

            val step2 = onBoardStepHandler.persistOnBoardStep2Data(inviterData)

            if (step2 != null) {
                updateViewState(OnBoardViewState.Idle)
                navigator.toOnBoardLightning(step2)
            } else {
                // TODO: Handle persistence error
                updateViewState(OnBoardViewState.Error)
            }
        }
    }
}
