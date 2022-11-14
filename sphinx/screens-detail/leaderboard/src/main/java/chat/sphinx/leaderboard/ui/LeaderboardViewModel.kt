package chat.sphinx.leaderboard.ui

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.leaderboard.ui.viewstate.LeaderboardViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class LeaderboardViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    ) : SideEffectViewModel<
        FragmentActivity,
        LeaderboardSideEffect,
        LeaderboardViewState>(dispatchers, LeaderboardViewState.Idle) {
}