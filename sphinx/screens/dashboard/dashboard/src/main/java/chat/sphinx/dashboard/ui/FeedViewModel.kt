package chat.sphinx.dashboard.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.concept_background_login.BackgroundLoginHandler
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject


@HiltViewModel
internal class FeedViewModel @Inject constructor(
    private val app: Application,
    private val backgroundLoginHandler: BackgroundLoginHandler,
    handler: SavedStateHandle,
    val dashboardNavigator: DashboardNavigator,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedSideEffect,
        FeedViewState
        >(dispatchers, FeedViewState.Default)
{

}
