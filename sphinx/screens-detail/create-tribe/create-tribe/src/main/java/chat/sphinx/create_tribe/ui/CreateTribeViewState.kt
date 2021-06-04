package chat.sphinx.create_tribe.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class CreateTribeViewState: ViewState<CreateTribeViewState>() {
    object Idle: CreateTribeViewState()
}
