package chat.sphinx.splash.ui

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import chat.sphinx.background_login.BackgroundLoginHandler
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
import io.matthewnelson.k_openssl_common.extensions.decodeToString
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.base64.decodeBase64ToArray
import org.cryptonode.jncryptor.AES256JNCryptor
import org.cryptonode.jncryptor.CryptorException
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val backgroundLoginHandler: BackgroundLoginHandler,
    private val navigator: SplashNavigator
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
            backgroundLoginHandler.attemptBackgroundLogin()?.let {
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
    fun decryptInput(decryptKeysViewState: OnBoardLayoutViewState.DecryptKeys, password: String?) {
        if (password == null || password.isEmpty()) {
            viewModelScope.launch(dispatchers.mainImmediate) {
                submitSideEffect(SplashSideEffect.InputNullOrEmpty)
            }
            return
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

                // TODO: Implement
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
