package chat.sphinx.chat_tribe.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class BadgesListViewState(): ViewState<BadgesListViewState>() {
    object Idle: BadgesListViewState()
}

