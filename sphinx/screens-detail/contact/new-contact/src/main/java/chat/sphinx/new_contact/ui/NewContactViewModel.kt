package chat.sphinx.new_contact.ui

import android.app.Application
import android.widget.ImageView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_subscription.SubscriptionRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.contact.ui.ContactSideEffect
import chat.sphinx.contact.ui.ContactViewModel
import chat.sphinx.contact.ui.ContactViewState
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.wrapper_contact.NewContact
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
import kotlinx.coroutines.flow.firstOrNull
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
    subscriptionRepository: SubscriptionRepository,
    walletDataHandler: WalletDataHandler,
    connectManager: ConnectManager,
    imageLoader: ImageLoader<ImageView>
): ContactViewModel<NewContactFragmentArgs>(
    newContactNavigator,
    dispatchers,
    app,
    contactRepository,
    subscriptionRepository,
    scannerCoordinator,
    walletDataHandler,
    connectManager,
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

    override fun createContact(
        contactAlias: ContactAlias,
        lightningNodePubKey: LightningNodePubKey,
        lightningRouteHint: LightningRouteHint?
    ) {
        if (saveContactJob?.isActive == true) {
            return
        }

        saveContactJob = viewModelScope.launch(mainImmediate) {
            val newContactIndex = contactRepository.getNewContactIndex().firstOrNull()
            val walletMnemonic = walletDataHandler.retrieveWalletMnemonic()

            val owner = contactRepository.accountOwner.value

            if (newContactIndex != null && walletMnemonic != null && lightningRouteHint != null && owner != null) {
                connectManager.createContact(
                    contactAlias.value,
                    lightningNodePubKey.value,
                    lightningRouteHint.value,
                    newContactIndex.value,
                    walletMnemonic,
                    owner.nodePubKey?.value ?: return@launch,
                    owner.routeHint?.value ?: return@launch,
                    owner.alias?.value ?: "",
                    owner.photoUrl?.value ?: ""
                )
                viewStateContainer.updateViewState(ContactViewState.Saved)
            }
        }
    }

    override fun storeContact(contact: NewContact) {
        viewModelScope.launch(mainImmediate) {
            contactRepository.createNewContact(contact)
        }
    }

    fun createContact(contact: NewContact){
        storeContact(contact)
    }

    /** Sphinx V1 (likely to be removed) **/

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
