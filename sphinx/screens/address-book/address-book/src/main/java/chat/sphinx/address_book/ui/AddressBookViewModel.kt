package chat.sphinx.address_book.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.isTrue
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

                val newList = ArrayList<Contact>(contacts.size)

                withContext(dispatchers.default) {
                    for (contact in contacts) {
                        if (contact.isOwner.isTrue()) {
                            continue
                        }
                    }
                }

                addressBookViewStateContainer.updateAddressBookContacts(newList.toList())
            }
        }
    }

}
