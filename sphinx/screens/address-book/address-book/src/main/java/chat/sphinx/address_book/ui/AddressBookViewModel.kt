package chat.sphinx.address_book.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.address_book.ui.adapter.AddressBookContact
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.isTrue
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal suspend inline fun AddressBookViewModel.collectAddressBookViewState(
    crossinline action: suspend (value: AddressBookViewState) -> Unit
): Unit =
    addressBookViewStateContainer.collect { action(it) }

internal val AddressBookViewModel.currentAddressBookViewState: AddressBookViewState
    get() = addressBookViewStateContainer.value

internal suspend inline fun AddressBookViewModel.updateAddressBookListFilter(filter: AddressBookFilter) {
    addressBookViewStateContainer.updateAddressBookContacts(null, filter)
}

@HiltViewModel
internal class AddressBookViewModel @Inject constructor(
    val dispatchers: CoroutineDispatchers,
    private val contactRepository: ContactRepository,
): BaseViewModel<AddressBookViewState>(AddressBookViewState.ListMode(listOf()))
{
    val addressBookViewStateContainer: AddressBookViewStateContainer by lazy {
        AddressBookViewStateContainer(dispatchers)
    }

    private val collectionLock = Mutex()

    init {
        viewModelScope.launch(dispatchers.mainImmediate) {
            contactRepository.getContacts().distinctUntilChanged().collect { contacts ->
                collectionLock.withLock {
                    if (contacts.isEmpty()) {
                        return@withLock
                    }

                    val newList = ArrayList<AddressBookContact>(contacts.size)

                    withContext(dispatchers.default) {
                        for (contact in contacts) {
                            if (contact.isOwner.isTrue()) {
                                continue
                            }

                            newList.add(AddressBookContact(contact))
                        }
                    }

                    addressBookViewStateContainer.updateAddressBookContacts(newList.toList())
                }
            }
        }
    }

}
