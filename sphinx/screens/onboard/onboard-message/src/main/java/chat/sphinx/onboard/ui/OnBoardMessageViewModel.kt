package chat.sphinx.onboard.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_signer_manager.SignerManager
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.onboard.navigation.OnBoardMessageNavigator
import chat.sphinx.onboard_common.OnBoardStepHandler
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayHMacKey
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.isOnionAddress
import chat.sphinx.wrapper_rsa.RsaPublicKey
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
internal class OnBoardMessageViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardMessageNavigator,
    private val onBoardStepHandler: OnBoardStepHandler,
    private val relayDataHandler: RelayDataHandler,
    private val walletDataHandler: WalletDataHandler,
    private val authenticationCoordinator: AuthenticationCoordinator
): SideEffectViewModel<
        Context,
        OnBoardMessageSideEffect,
        OnBoardMessageViewState
        >(dispatchers, OnBoardMessageViewState.Idle)
{

    private lateinit var signerManager: SignerManager

    fun setSignerManager(signerManager: SignerManager) {
        signerManager.setWalletDataHandler(walletDataHandler)

        this.signerManager = signerManager
    }

    private var loginJob: Job? = null
    fun presentLoginModal(
        relayUrl: RelayUrl,
        authToken: AuthorizationToken,
        transportKey: RsaPublicKey?,
        hMacKey: RelayHMacKey?,
        inviterData: OnBoardInviterData,
    ) {
        if (loginJob?.isActive == true || proceedJob?.isActive == true) {
            return
        }

        updateViewState(OnBoardMessageViewState.Saving)

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
                                OnBoardMessageSideEffect.RelayUrlHttpConfirmation(
                                    relayUrl = relayUrl,
                                    callback = { url ->
                                        if (url != null) {
                                            proceedToLightningScreen(
                                                relayUrl,
                                                authToken,
                                                transportKey,
                                                hMacKey,
                                                inviterData
                                            )
                                        } else {
                                            // cancelled
                                            updateViewState(OnBoardMessageViewState.Idle)
                                        }
                                    }
                                )
                            )
                        } else {
                            proceedToLightningScreen(
                                relayUrl,
                                authToken,
                                transportKey,
                                hMacKey,
                                inviterData
                            )
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
        relayUrl: RelayUrl,
        authorizationToken: AuthorizationToken,
        transportKey: RsaPublicKey?,
        hMacKey: RelayHMacKey?,
        inviterData: OnBoardInviterData,
    ) {
        if (proceedJob?.isActive == true) {
            return
        }

        proceedJob = viewModelScope.launch(mainImmediate) {
            relayDataHandler.persistAuthorizationToken(authorizationToken)
            relayDataHandler.persistRelayUrl(relayUrl)
            signerManager.persistMnemonic()

            transportKey?.let { key ->
                relayDataHandler.persistRelayTransportKey(key)
            }

            hMacKey?.let { key ->
                relayDataHandler.persistRelayHMacKey(key)
            }

            val step2 = onBoardStepHandler.persistOnBoardStep2Data(inviterData)

            if (step2 != null) {
                updateViewState(OnBoardMessageViewState.Idle)
                navigator.toOnBoardLightning(step2)
            } else {
                // TODO: Handle persistence error
                updateViewState(OnBoardMessageViewState.Error)
            }
        }
    }
}
