package chat.sphinx.splash.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.key_restore.KeyRestore
import chat.sphinx.splash.navigation.SplashNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.coordinator.AuthenticationResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.k_openssl_common.clazzes.Password
import io.matthewnelson.k_openssl_common.extensions.decodeToString
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.base64.decodeBase64ToArray
import org.cryptonode.jncryptor.AES256JNCryptor
import org.cryptonode.jncryptor.CryptorException
import javax.annotation.meta.Exhaustive
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
        >(SplashViewState.Idle)
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
                navigator.toHomeScreen()
            } ?: let {
                if (authenticationCoordinator.isAnEncryptionKeySet()) {
                    authenticationCoordinator.submitAuthenticationRequest(
                        AuthenticationRequest.LogIn()
                    ).first().let { response ->
                        @Exhaustive
                        when (response) {
                            is AuthenticationResponse.Failure -> {
                                // will not be returned as back press for handling
                                // a LogIn request minimizes the application until
                                // User has authenticated
                            }
                            is AuthenticationResponse.Success.Authenticated -> {
                                // Prime relay cache data before navigating (takes an extra ~ .3s)
                                navigator.toHomeScreen()
                            }
                            is AuthenticationResponse.Success.Key -> {
                                // will never be returned
                            }
                        }
                    }
                } else {
                    // Display OnBoard
                    updateViewState(SplashViewState.StartScene)
                }
            }
        }
    }

    // TODO: Limit to not cause buffer overflow and crash.
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
                        layoutViewStateContainer.updateViewState(
                            OnBoardLayoutViewState.DecryptKeys(toDecryptByteArray)
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
    fun decryptInput(
        decryptKeysViewState: OnBoardLayoutViewState.DecryptKeys,
        password: String?
    ) {
        if (password == null || password.isEmpty()) {
            viewModelScope.launch(dispatchers.mainImmediate) {
                submitSideEffect(SplashSideEffect.InputNullOrEmpty)
            }
            return
        }

        // TODO: Replace with automatic launching upon entering the 6th PIN character
        //  when Authentication View's Layout gets incorporated
        if (password.length != 6 /*TODO: https://github.com/stakwork/sphinx-kotlin/issues/9*/) {
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
                val decryptedSplit = AES256JNCryptor()
                    .decryptData(decryptKeysViewState.toDecrypt, password.toCharArray())
                    .decodeToString()
                    .split("::")

                if (decryptedSplit.size != 4) {
                    throw IllegalArgumentException("Decrypted keys do not contain enough arguments")
                }

                // TODO: Ask to use Tor before any network calls go out.
                // TODO: Hit relayUrl to verify creds work

                keyRestore.restoreKeys(
                    privateKey = Password(decryptedSplit[0].toCharArray()),
                    publicKey = Password(decryptedSplit[1].toCharArray()),
                    userPin = password.toCharArray(),
                    relayUrl = decryptedSplit[2],
                    jwt = decryptedSplit[3],
                ).collect { flowResponse ->
                    // TODO: Implement in Authentication View when it get's built/refactored
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
                exception.printStackTrace()
                submitSideEffect(SplashSideEffect.DecryptionFailure)
            }
        }
    }

    @Suppress("RemoveExplicitTypeArguments")
    val layoutViewStateContainer: ViewStateContainer<OnBoardLayoutViewState> by lazy {
        ViewStateContainer<OnBoardLayoutViewState>(OnBoardLayoutViewState.Hidden)
    }

    // Unused
    override suspend fun onMotionSceneCompletion(value: Any) {
        return
    }
}
