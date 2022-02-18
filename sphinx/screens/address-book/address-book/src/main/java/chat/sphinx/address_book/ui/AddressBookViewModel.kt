package chat.sphinx.address_book.ui

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import chat.sphinx.address_book.R
import chat.sphinx.address_book.navigation.AddressBookNavigator
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.isTrue
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class AddressBookViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    var addressBookNavigator: AddressBookNavigator,
    private val contactRepository: ContactRepository,
): SideEffectViewModel<
        FragmentActivity,
        AddressBookSideEffect,
        AddressBookViewState,
        >(dispatchers, AddressBookViewState.ListMode(listOf(), listOf()))
{

    private val addressBookViewStateContainer: AddressBookViewStateContainer by lazy {
        AddressBookViewStateContainer(dispatchers)
    }

    override val viewStateContainer: ViewStateContainer<AddressBookViewState>
        get() = addressBookViewStateContainer

    suspend fun updateAddressBookListFilter(filter: AddressBookFilter) {
        addressBookViewStateContainer.updateAddressBookContacts(null, filter)
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch(mainImmediate) {
            contactRepository.deleteContactById(contact.id)
        }
    }

    fun blockContact(contact: Contact) {
        viewModelScope.launch(mainImmediate) {
            contactRepository.toggleContactBlocked(contact)
        }
    }

    fun onItemLongClick() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(AddressBookSideEffect.Notify(
                app.getString(R.string.swipe_left_to_delete))
            )
        }
    }

    init {
        viewModelScope.launch(mainImmediate) {
            contactRepository.getAllContacts.distinctUntilChanged().collect { contacts ->
                if (contacts.isEmpty()) {
                    return@collect
                }

                val mutableList = contacts.toMutableList()

                withContext(default) {
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
