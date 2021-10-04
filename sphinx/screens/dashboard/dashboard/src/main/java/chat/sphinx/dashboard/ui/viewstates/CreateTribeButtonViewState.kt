package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class CreateTribeButtonViewState: ViewState<CreateTribeButtonViewState>() {

    object Visible : CreateTribeButtonViewState()

    object Hidden : CreateTribeButtonViewState()
}