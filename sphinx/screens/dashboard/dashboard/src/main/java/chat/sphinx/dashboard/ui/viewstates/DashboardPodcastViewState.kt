package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DashboardPodcastViewState: ViewState<DashboardPodcastViewState>() {
    object Idle: DashboardPodcastViewState()
}