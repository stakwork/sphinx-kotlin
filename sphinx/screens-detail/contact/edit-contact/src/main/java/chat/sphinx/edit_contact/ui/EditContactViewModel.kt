package chat.sphinx.edit_contact.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_contact.model.ContactForm
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.contact.ui.ContactSideEffect
import chat.sphinx.contact.ui.ContactViewModel
import chat.sphinx.contact.ui.ContactViewState
import chat.sphinx.edit_contact.navigation.EditContactNavigator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_contact.getColorKey
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EditContactViewModel @Inject constructor(
    editContactNavigator: EditContactNavigator,
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    app: Application,
    scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    contactRepository: ContactRepository
): ContactViewModel<EditContactFragmentArgs>(
    editContactNavigator,
    dispatchers,
    app,
    contactRepository,
    scannerCoordinator
)
{
    override val args: EditContactFragmentArgs by savedStateHandle.navArgs()

    override val fromAddFriend: Boolean
        get() = false
    override val contactId: ContactId
        get() = ContactId(args.argContactId)

    override fun initContactDetails() {
        viewModelScope.launch(mainImmediate) {
            contactRepository.getContactById(contactId).collectLatest { contact ->
                if (contact != null) {
                    contact.nodePubKey?.let { lightningNodePubKey ->
                        submitSideEffect(
                            ContactSideEffect.ExistingContact(
                                contact.alias?.value,
                                contact.photoUrl,
                                contact.getColorKey(),
                                lightningNodePubKey,
                                contact.routeHint
                            )
                        )
                    }
                }
            }
        }
    }

    override fun saveContact(contactForm: ContactForm) {
        viewModelScope.launch(mainImmediate) {
            viewStateContainer.updateViewState(ContactViewState.Saving)

            val loadResponse = contactRepository.updateContact(
                contactId,
                contactForm.contactAlias,
                contactForm.lightningRouteHint
            )

            when (loadResponse) {
                is Response.Error -> {
                    viewStateContainer.updateViewState(ContactViewState.Error)
                }
                is Response.Success -> {
                    viewStateContainer.updateViewState(ContactViewState.Saved)
                }
            }
        }
    }
}
