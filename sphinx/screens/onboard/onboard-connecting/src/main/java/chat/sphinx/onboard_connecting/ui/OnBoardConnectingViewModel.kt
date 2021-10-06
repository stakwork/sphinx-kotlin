package chat.sphinx.onboard_connecting.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.onboard_connecting.navigation.OnBoardConnectingNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

internal inline val OnBoardConnectingFragmentArgs.newUser: Boolean
    get() = argNewUser

@HiltViewModel
internal class OnBoardConnectingViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
    private val navigator: OnBoardConnectingNavigator,
): SideEffectViewModel<
        Context,
        OnBoardConnectingSideEffect,
        OnBoardConnectingViewState
        >(dispatchers, OnBoardConnectingViewState.Idle)
{

    private val args: OnBoardConnectingFragmentArgs by handle.navArgs()
}