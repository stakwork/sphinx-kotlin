package chat.sphinx.dashboard.ui.feed.watch

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.FeedReadViewState
import chat.sphinx.dashboard.ui.viewstates.FeedWatchViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject


@HiltViewModel
internal class FeedWatchViewModel @Inject constructor(
    handler: SavedStateHandle,
    val dashboardNavigator: DashboardNavigator,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedWatchSideEffect,
        FeedWatchViewState
        >(dispatchers, FeedWatchViewState.Idle)
{

}
