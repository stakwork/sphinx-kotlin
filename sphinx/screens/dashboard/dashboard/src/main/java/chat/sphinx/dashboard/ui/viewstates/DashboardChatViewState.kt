package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DashboardChatViewState: ViewState<DashboardChatViewState>() {
    object Idle: DashboardChatViewState()
}
