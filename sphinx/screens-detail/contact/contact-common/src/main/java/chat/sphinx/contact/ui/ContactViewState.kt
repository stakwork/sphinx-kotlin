package chat.sphinx.contact.ui

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class ContactViewState: ViewState<ContactViewState>() {
    object Idle: ContactViewState()
    object Saving: ContactViewState()
    object Saved: ContactViewState()
    object Error: ContactViewState()
}
