package chat.sphinx.edit_contact.ui

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
import chat.sphinx.edit_contact.navigation.EditContactNavigator
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.kotlin_response.Response
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.getColorKey
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EditContactViewModel @Inject constructor(
    editContactNavigator: EditContactNavigator,
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
    imageLoader: ImageLoader<ImageView>,
): ContactViewModel<EditContactFragmentArgs>(
    editContactNavigator,
    dispatchers,
    app,
    contactRepository,
    subscriptionRepository,
    scannerCoordinator,
    walletDataHandler,
    connectManagerRepository,
    moshi,
    lightningRepository,
    imageLoader
)
{
    override val args: EditContactFragmentArgs by savedStateHandle.navArgs()

    override val fromAddFriend: Boolean
        get() = false
    override val contactId: ContactId
        get() = ContactId(args.argContactId)

    override fun initContactDetails() {
        viewModelScope.launch(mainImmediate) {
            contactRepository.getContactById(contactId).firstOrNull().let { contact ->
                if (contact != null) {
                    contact.nodePubKey?.let { lightningNodePubKey ->

                        val subscription = subscriptionRepository.getActiveSubscriptionByContactId(
                            contactId
                        ).firstOrNull()

                        submitSideEffect(
                            ContactSideEffect.ExistingContact(
                                contact.alias?.value,
                                contact.photoUrl,
                                contact.getColorKey(),
                                lightningNodePubKey,
                                contact.routeHint,
                                subscription != null
                            )
                        )
                    }
                }
            }
        }
    }

    override fun createContact(
        contactAlias: ContactAlias,
        lightningNodePubKey: LightningNodePubKey,
        lightningRouteHint: LightningRouteHint?
    ) {}

    /** Sphinx V1 (likely to be removed) **/

    suspend fun toSubscriptionDetailScreen() {
        (navigator as EditContactNavigator).toSubscribeDetailScreen(contactId)
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
            viewStateContainer.updateViewState(ContactViewState.Saving)

            val loadResponse = contactRepository.updateContact(
                contactId,
                contactAlias,
                lightningRouteHint
            )

            when (loadResponse) {
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
