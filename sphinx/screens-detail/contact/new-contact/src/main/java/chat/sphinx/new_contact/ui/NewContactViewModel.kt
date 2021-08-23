package chat.sphinx.new_contact.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_contact.model.ContactForm
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.contact.ui.ContactSideEffect
import chat.sphinx.contact.ui.ContactViewModel
import chat.sphinx.contact.ui.ContactViewState
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.new_contact.navigation.NewContactNavigator
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toLightningRouteHint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class NewContactViewModel @Inject constructor(
    newContactNavigator: NewContactNavigator,
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    app: Application,
    scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    contactRepository: ContactRepository
): ContactViewModel<NewContactFragmentArgs>(
    newContactNavigator,
    dispatchers,
    app,
    contactRepository,
    scannerCoordinator
) {
    override val args: NewContactFragmentArgs by savedStateHandle.navArgs()

    override val fromAddFriend: Boolean
        get() = args.argFromAddFriend
    override val contactId: ContactId?
        get() = null

    override fun initContactDetails() {

        args.argPubKey?.toLightningNodePubKey()?.let { lightningNodePubKey ->
            val lightningRouteHint = args.argRouteHint?.toLightningRouteHint()

            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    ContactSideEffect.ContactInfo(
                        lightningNodePubKey,
                        lightningRouteHint
                    )
                )
            }
        }
    }

    override fun saveContact(contactForm: ContactForm) {
        viewModelScope.launch(mainImmediate) {
            contactRepository.createContact(
                contactForm.contactAlias,
                contactForm.lightningNodePubKey,
                contactForm.lightningRouteHint
            ).collect { loadResponse ->
                @app.cash.exhaustive.Exhaustive
                when(loadResponse) {
                    LoadResponse.Loading -> {
                        viewStateContainer.updateViewState(ContactViewState.Saving)
                    }
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
}
