package chat.sphinx.splash.ui

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import chat.sphinx.splash.navigation.SplashNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
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

    fun processUserInput(input: String) {
        // If invite code
        // If account restore
        // else error
    }

    // TODO: Temporary until Authentication gets built out.
    init {
        viewModelScope.launch(dispatchers.mainImmediate) {
            // need a slight delay for window hand over
            delay(375L)
            updateViewState(SplashViewState.StartScene)
        }
    }

    // Unused
    override suspend fun onMotionSceneCompletion(value: Any) {
        return
    }
}