package chat.sphinx.common_player.ui

import androidx.fragment.app.FragmentActivity
import chat.sphinx.common_player.viewstate.CommonPlayerScreenViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class CommonPlayerScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers
    ):SideEffectViewModel<
        FragmentActivity,
        CommonPlayerScreenSideEffect,
        CommonPlayerScreenViewState,
        >(dispatchers, CommonPlayerScreenViewState.Idle) {}