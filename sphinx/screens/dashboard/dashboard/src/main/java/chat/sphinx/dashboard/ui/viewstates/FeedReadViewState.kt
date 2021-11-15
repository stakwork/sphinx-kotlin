package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedReadViewState: ViewState<FeedReadViewState>() {

    object Idle: FeedReadViewState()
}