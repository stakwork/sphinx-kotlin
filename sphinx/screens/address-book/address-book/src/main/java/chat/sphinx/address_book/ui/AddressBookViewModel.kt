package chat.sphinx.address_book.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
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

    fun deleteContact(deletedContact: Contact) {
        viewModelScope.launch(dispatchers.mainImmediate) {
            contactRepository.deleteContactById(deletedContact.id)
        }
    }

    init {
        viewModelScope.launch(dispatchers.mainImmediate) {
            contactRepository.getContacts().distinctUntilChanged().collect { contacts ->
                if (contacts.isEmpty()) {
                    return@collect
                }

                val mutableList = contacts.toMutableList()

                withContext(dispatchers.default) {
                    for ((index, contact) in contacts.withIndex()) {
                        if (contact.isOwner.isTrue()) {
                            mutableList.removeAt(index)
                            break
                        }
                    }
                }

                addressBookViewStateContainer.updateAddressBookContacts(mutableList.toList())
            }
        }
    }

}
