package chat.sphinx.chat_tribe.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.chat_tribe.ui.viewstate.BadgesListViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
class BadgesListViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        BadgesListSideEffect,
        BadgesListViewState,
        >(dispatchers, BadgesListViewState.Idle) {


}
