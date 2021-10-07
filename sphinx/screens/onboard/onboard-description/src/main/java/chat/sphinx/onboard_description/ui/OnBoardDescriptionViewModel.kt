package chat.sphinx.onboard_description.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.onboard_description.navigation.OnBoardDescriptionNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val OnBoardDescriptionFragmentArgs.newUser: Boolean
    get() = argNewUser

@HiltViewModel
internal class OnBoardDescriptionViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
    private val navigator: OnBoardDescriptionNavigator,
): SideEffectViewModel<
        Context,
        OnBoardDescriptionSideEffect,
        OnBoardDescriptionViewState
        >(dispatchers, OnBoardDescriptionViewState.Idle)
{

    private val args: OnBoardDescriptionFragmentArgs by handle.navArgs()

    init {
        updateViewState(
            if (args.newUser) {
                OnBoardDescriptionViewState.NewUser
            } else {
                OnBoardDescriptionViewState.ExistingUser
            }
        )
    }

    fun nextScreen() {
        viewModelScope.launch(mainImmediate) {
            navigator.toOnBoardConnectScreen(args.newUser)
        }
    }
}