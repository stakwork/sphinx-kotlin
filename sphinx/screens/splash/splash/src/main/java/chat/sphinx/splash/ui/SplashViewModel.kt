package chat.sphinx.splash.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.key_restore.KeyRestore
import chat.sphinx.key_restore.KeyRestoreResponse
import chat.sphinx.splash.navigation.SplashNavigator
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.crypto_common.extensions.decodeToString
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okio.base64.decodeBase64ToArray
import org.cryptonode.jncryptor.AES256JNCryptor
import org.cryptonode.jncryptor.CryptorException
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val backgroundLoginHandler: BackgroundLoginHandler,
    private val dispatchers: CoroutineDispatchers,
    private val keyRestore: KeyRestore,
    private val navigator: SplashNavigator,
): MotionLayoutViewModel<
        Any,
        Context,
        SplashSideEffect,
        SplashViewState
        >(SplashViewState.Start_ShowIcon)
{
    private var screenInit: Boolean = false
    fun screenInit() {
        if (screenInit) {
            return
        } else {
            screenInit = true
        }

        viewModelScope.launch(dispatchers.mainImmediate) {
            backgroundLoginHandler.attemptBackgroundLogin(
                updateLastLoginTimeOnSuccess = true
            )?.let {
                navigator.toDashboardScreen()
            } ?: let {
                if (authenticationCoordinator.isAnEncryptionKeySet()) {
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
                                // Prime relay cache data before navigating (takes an extra ~ .3s)
                                // TODO: Enable once SplashFragment backpress handling is cleaned up
//                                backgroundLoginHandler.updateLoginTime()
                                navigator.toDashboardScreen()
                            }
                            is AuthenticationResponse.Success.Key -> {
                                // will never be returned
                            }
                        }
                    }
                } else {
                    // Display OnBoard
                    delay(100L) // need a slight delay for window to fully hand over to splash
                    updateViewState(SplashViewState.Transition_Set2_ShowWelcome)
                }
            }
        }
    }

    // TODO: Use coordinator pattern and limit
    fun navigateToScanner() {
        viewModelScope.launch(dispatchers.mainImmediate) {
            submitSideEffect(SplashSideEffect.NotImplementedYet)
        }
    }

    fun processUserInput(input: String?) {
        if (input.isNullOrEmpty()) {
            viewModelScope.launch(dispatchers.mainImmediate) {
                submitSideEffect(SplashSideEffect.InputNullOrEmpty)
            }
            return
        }

        // Invite Code
        if (input.length == 40) {
            // TODO: Implement
            viewModelScope.launch(dispatchers.mainImmediate) {
                submitSideEffect(SplashSideEffect.NotImplementedYet)
            }
            return
        }

        input.decodeBase64ToArray()?.decodeToString()?.split("::")?.let { decodedSplit ->
            if (decodedSplit.size == 2) {

                if (decodedSplit.elementAt(0) == "ip") {
                    // TODO: Implement
                    viewModelScope.launch(dispatchers.mainImmediate) {
                        submitSideEffect(SplashSideEffect.NotImplementedYet)
                    }
                    return
                }

                if (decodedSplit.elementAt(0) == "keys") {
                    decodedSplit.elementAt(1).decodeBase64ToArray()?.let { toDecryptByteArray ->
                        updateViewState(
                            SplashViewState.Transition_Set3_DecryptKeys(toDecryptByteArray)
                        )
                        return
                    }
                }

            } // input not properly formatted `type::data`
        }

        viewModelScope.launch(dispatchers.mainImmediate) {
            submitSideEffect(SplashSideEffect.InvalidCode)
        }
    }

    private var decryptionJob: Job? = null
    fun decryptInput(viewState: SplashViewState.Set3_DecryptKeys) {
        // TODO: Replace with automatic launching upon entering the 6th PIN character
        //  when Authentication View's Layout gets incorporated
        if (viewState.pinWriter.size() != 6 /*TODO: https://github.com/stakwork/sphinx-kotlin/issues/9*/) {
            viewModelScope.launch(dispatchers.mainImmediate) {
                submitSideEffect(SplashSideEffect.InvalidPinLength)
            }
        }

        if (decryptionJob?.isActive == true) {
            return
        }

        var decryptionJobException: Exception? = null
        decryptionJob = viewModelScope.launch(dispatchers.default) {
            try {
                val pin = viewState.pinWriter.toCharArray()
                val decryptedSplit = AES256JNCryptor()
                    .decryptData(viewState.toDecrypt, pin)
                    .decodeToString()
                    .split("::")

                if (decryptedSplit.size != 4) {
                    throw IllegalArgumentException("Decrypted keys do not contain enough arguments")
                }

                // TODO: Ask to use Tor before any network calls go out.
                // TODO: Hit relayUrl to verify creds work

                var success: KeyRestoreResponse.Success? = null
                keyRestore.restoreKeys(
                    privateKey = Password(decryptedSplit[0].toCharArray()),
                    publicKey = Password(decryptedSplit[1].toCharArray()),
                    userPin = pin,
                    relayUrl = RelayUrl(decryptedSplit[2]),
                    authorizationToken = AuthorizationToken(decryptedSplit[3]),
                ).collect { flowResponse ->
                    // TODO: Implement in Authentication View when it get's built/refactored
                    if (flowResponse is KeyRestoreResponse.Success) {
                        success = flowResponse
                    }
                }

                success?.let { successResponse ->
                    // Overwrite PIN
                    viewState.pinWriter.reset()
                    repeat(6) {
                        viewState.pinWriter.append('0')
                    }

                    // TODO: Enable once SplashFragment backpress handling is cleaned up
//                    backgroundLoginHandler.updateLoginTime()
                    navigator.toDashboardScreen()

                } ?: updateViewState(
                    SplashViewState.Set3_DecryptKeys(viewState.toDecrypt)
                ).also {
                    submitSideEffect(SplashSideEffect.InvalidPin)
                }

                // TODO: on success, show snackbar to clear clipboard
            } catch (e: CryptorException) {
                decryptionJobException = e
            } catch (e: IllegalArgumentException) {
                decryptionJobException = e
            }
        }

        viewModelScope.launch(dispatchers.mainImmediate) {
            decryptionJob?.join()
            decryptionJobException?.let { exception ->
                updateViewState(
                    // reset view state
                    SplashViewState.Set3_DecryptKeys(viewState.toDecrypt)
                )
                exception.printStackTrace()
                submitSideEffect(SplashSideEffect.DecryptionFailure)
            }
        }
    }

    // Unused
    override suspend fun onMotionSceneCompletion(value: Any) {
        return
    }
}
