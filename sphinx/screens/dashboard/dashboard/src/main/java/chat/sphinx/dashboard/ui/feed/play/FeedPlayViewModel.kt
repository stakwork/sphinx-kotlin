package chat.sphinx.dashboard.ui.feed.play

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.FeedPlayViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject


@HiltViewModel
internal class FeedPlayViewModel @Inject constructor(
    handler: SavedStateHandle,
    val dashboardNavigator: DashboardNavigator,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedPlaySideEffect,
        FeedPlayViewState
        >(dispatchers, FeedPlayViewState.Idle)
{

}
