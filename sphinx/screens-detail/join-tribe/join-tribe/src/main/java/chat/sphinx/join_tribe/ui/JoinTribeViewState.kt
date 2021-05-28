package chat.sphinx.join_tribe.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class JoinTribeViewState: ViewState<JoinTribeViewState>() {
    object Idle: JoinTribeViewState()
}
