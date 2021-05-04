package chat.sphinx.new_contact.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class NewContactViewState: ViewState<NewContactViewState>() {
    object Idle: NewContactViewState()
}
