package chat.sphinx.contact_detail.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ContactDetailViewState: ViewState<ContactDetailViewState>() {
    object Idle: ContactDetailViewState()
}
