package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class DashboardTabsViewState: ViewState<DashboardTabsViewState>() {

    object Idle: DashboardTabsViewState()

    data class TabsState(
        val feedActive: Boolean,
        val friendsActive: Boolean,
        val tribesActive: Boolean,
        val friendsBadgeVisible: Boolean,
        val tribesBadgeVisible: Boolean,
    ) : DashboardTabsViewState()
}