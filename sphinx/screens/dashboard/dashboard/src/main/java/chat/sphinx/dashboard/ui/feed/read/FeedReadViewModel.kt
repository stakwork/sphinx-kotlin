package chat.sphinx.dashboard.ui.feed.read

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.FeedReadViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject


@HiltViewModel
internal class FeedReadViewModel @Inject constructor(
    handler: SavedStateHandle,
    val dashboardNavigator: DashboardNavigator,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedReadSideEffect,
        FeedReadViewState
        >(dispatchers, FeedReadViewState.Idle)
{

}
