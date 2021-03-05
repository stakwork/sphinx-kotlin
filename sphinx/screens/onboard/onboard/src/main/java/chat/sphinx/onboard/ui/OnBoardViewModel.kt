package chat.sphinx.onboard.ui

import android.content.Context
import chat.sphinx.onboard.navigation.OnBoardNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import javax.inject.Inject

@HiltViewModel
internal class OnBoardViewModel @Inject constructor(
    val navigator: OnBoardNavigator
): SideEffectViewModel<
        Context,
        OnBoardSideEffect,
        OnBoardViewState
        >(OnBoardViewState.Idle)
{
}
