package chat.sphinx.new_contact.ui

import android.app.Application
import android.widget.ImageView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_repository_connect_manager.ConnectManagerRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_subscription.SubscriptionRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.concept_wallet.WalletDataHandler
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
import chat.sphinx.wrapper_contact.NewContact
import com.squareup.moshi.Moshi
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
    connectManagerRepository: ConnectManagerRepository,
    moshi: Moshi,
    lightningRepository: LightningRepository,
    imageLoader: ImageLoader<ImageView>
): ContactViewModel<NewContactFragmentArgs>(
    newContactNavigator,
    dispatchers,
    app,
    contactRepository,
    subscriptionRepository,
    scannerCoordinator,
    walletDataHandler,
    connectManagerRepository,
    moshi,
    lightningRepository,
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

        args.argPubKey?.toLightningNodePubKey()?.let { pubKey ->
            args.argRouteHint?.toLightningRouteHint()?.let { routeHint ->
                initContactJob = viewModelScope.launch(mainImmediate) {
                    submitSideEffect(
                        ContactSideEffect.ContactInfo(
                            pubKey,
                            routeHint
                        )
                    )
                }
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
            val exitingContact = contactRepository.getContactByPubKey(lightningNodePubKey).firstOrNull()

            if (lightningRouteHint != null && exitingContact == null) {

                val newContact = NewContact(
                    contactAlias,
                    lightningNodePubKey,
                    lightningRouteHint,
                    null,
                    false,
                    null,
                    null,
                    null
                )
                connectManagerRepository.createContact(newContact)
                viewStateContainer.updateViewState(ContactViewState.Saved)
            }
        }
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
