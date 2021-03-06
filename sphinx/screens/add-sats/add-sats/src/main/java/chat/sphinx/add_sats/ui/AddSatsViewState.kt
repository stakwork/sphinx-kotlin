package chat.sphinx.add_sats.ui

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class AddSatsViewState: ViewState<AddSatsViewState>() {
    object Idle: AddSatsViewState()
}