package chat.sphinx.address_book.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class AddressBookViewState: ViewState<AddressBookViewState>() {
    object Idle: AddressBookViewState()
}