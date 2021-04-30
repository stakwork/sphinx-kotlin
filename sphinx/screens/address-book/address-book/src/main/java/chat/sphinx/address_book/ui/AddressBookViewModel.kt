package chat.sphinx.address_book.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.address_book.ui.adapter.AddressBookContact
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.wrapper_contact.isTrue
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class AddressBookViewModel @Inject constructor(
    val dispatchers: CoroutineDispatchers,
    private val contactRepository: ContactRepository,
): BaseViewModel<AddressBookViewState>(AddressBookViewState.ListMode(listOf()))
{
    private val addressBookViewStateContainer: AddressBookViewStateContainer by lazy {
        AddressBookViewStateContainer(dispatchers)
    }

    override val viewStateContainer: ViewStateContainer<AddressBookViewState>
        get() = addressBookViewStateContainer

    suspend fun updateAddressBookListFilter(filter: AddressBookFilter) {
        addressBookViewStateContainer.updateAddressBookContacts(null, filter)
    }

    init {
        viewModelScope.launch(dispatchers.mainImmediate) {
            contactRepository.getContacts().distinctUntilChanged().collect { contacts ->
                if (contacts.isEmpty()) {
                    return@collect
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
