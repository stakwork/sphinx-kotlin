package chat.sphinx.splash.ui

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import chat.sphinx.splash.navigation.SplashNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.k_openssl_common.extensions.decodeToString
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.base64.decodeBase64ToArray
import org.cryptonode.jncryptor.AES256JNCryptor
import org.cryptonode.jncryptor.CryptorException
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val authenticationStorage: AuthenticationStorage,
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

        val initTime = SystemClock.uptimeMillis()
        // check for credentials from authentication prefs
        // TODO: ask if private mode persists across app restart. If so, overwrite creds with
        //  "PRIVATE_MODE" value on logout to ensure re-logging in always
        //  requires a pin for private mode

        // TODO: Build API for authentication coordinator/manager to check if credentials are set
            // if background logout creds not null (we know user account exists)
                // if time last login - initTime > logout setting
                    // remove from storage
                    // request login
                // else
                    // re-save with new time
                    // send login request with key (TODO: Build API out for authentication coordinator/manager)
            // else (we don't know if user account exists)
                // send request to see if encryption key exists in storage (TODO: Build API for authentication coordinator/manager)
                    // if exists
                        // request encryption key (auto navigate to login)
                        // if user setting for background logout is not 0
                            // save key with new time
                        // navigate to home screen
                    // if doesn't exist
                        // start onboard anim
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

                OnBoardLayoutViewState.StringInputType.fromString(decodedSplit.elementAt(0))?.let { type ->

                    if (type is OnBoardLayoutViewState.StringInputType.Ip) {
                        // TODO: Implement
                        viewModelScope.launch(dispatchers.mainImmediate) {
                            submitSideEffect(SplashSideEffect.NotImplementedYet)
                        }
                        return
                    }

                    decodedSplit.elementAt(1).decodeBase64ToArray()?.let { toDecryptByteArray ->

                        layoutViewStateContainer.updateViewState(
                            OnBoardLayoutViewState.Decrypt(
                                type,
                                toDecryptByteArray
                            )
                        )
                        return
                    } // data to decrypt was not base64 encoded

                } // input type not recognized

            } // input not properly formatted `type::data`
        }

        viewModelScope.launch(dispatchers.mainImmediate) {
            submitSideEffect(SplashSideEffect.InvalidCode)
        }
    }

    private var decryptionJob: Job? = null
    fun decryptInput(decryptViewState: OnBoardLayoutViewState.Decrypt, password: String?) {
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
                    .decryptData(decryptViewState.toDecrypt, password.toCharArray())
                    .decodeToString()
                    .split("::")

                if (
                    decryptViewState.stringInputType is OnBoardLayoutViewState.StringInputType.Keys &&
                    decryptedSplit.size != 4
                ) {
                    throw IllegalArgumentException(
                        "Not enough arguments for decrypted StringInputType - " +
                                decryptViewState.stringInputType.value
                    )
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

    // TODO: Temporary until Authentication gets built out.
    init {
        viewModelScope.launch(dispatchers.mainImmediate) {
            // need a slight delay for window hand over
            delay(375L)
            updateViewState(SplashViewState.StartScene)
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