package chat.sphinx.onboard.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.onboard.navigation.OnBoardNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class OnBoardViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: OnBoardNavigator
): SideEffectViewModel<
        Context,
        OnBoardSideEffect,
        OnBoardViewState
        >(dispatchers, OnBoardViewState.Idle)
{
}
