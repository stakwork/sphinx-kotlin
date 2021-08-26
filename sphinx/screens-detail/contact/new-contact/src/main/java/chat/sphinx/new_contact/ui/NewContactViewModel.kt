package chat.sphinx.new_contact.ui

import android.app.Application
import android.widget.ImageView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_repository_contact.ContactRepository
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
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toLightningRouteHint
import chat.sphinx.wrapper_contact.ContactAlias
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
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
    contactRepository: ContactRepository,
    imageLoader: ImageLoader<ImageView>
): ContactViewModel<NewContactFragmentArgs>(
    newContactNavigator,
    dispatchers,
    app,
    contactRepository,
    scannerCoordinator,
    imageLoader,
) {
    override val args: NewContactFragmentArgs by savedStateHandle.navArgs()

    override val fromAddFriend: Boolean
        get() = args.argFromAddFriend
    override val contactId: ContactId?
        get() = null

    private var initContactJob: Job? = null
    override fun initContactDetails() {
        if (initContactJob?.isActive == true) {
            return
        }

        args.argPubKey?.toLightningNodePubKey()?.let { lightningNodePubKey ->
            val lightningRouteHint = args.argRouteHint?.toLightningRouteHint()

            initContactJob = viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    ContactSideEffect.ContactInfo(
                        lightningNodePubKey,
                        lightningRouteHint
                    )
                )
            }
        }
    }

    override fun saveContact(
        contactAlias: ContactAlias,
        lightningNodePubKey: LightningNodePubKey,
        lightningRouteHint: LightningRouteHint?
    ) {
        if (saveContactJob?.isActive == true) {
            return
        }

        saveContactJob = viewModelScope.launch(mainImmediate) {
            contactRepository.createContact(
                contactAlias,
                lightningNodePubKey,
                lightningRouteHint
            ).collect { loadResponse ->
                @app.cash.exhaustive.Exhaustive
                when(loadResponse) {
                    LoadResponse.Loading -> {
                        viewStateContainer.updateViewState(ContactViewState.Saving)
                    }
                    is Response.Error -> {
                        submitSideEffect(ContactSideEffect.Notify.FailedToSaveContact)

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
