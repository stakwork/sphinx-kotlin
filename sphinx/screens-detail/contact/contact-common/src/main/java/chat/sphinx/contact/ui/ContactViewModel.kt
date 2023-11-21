package chat.sphinx.contact.ui

import android.app.Application
import android.content.Context
import android.widget.ImageView
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavArgs
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_subscription.SubscriptionRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.contact.R
import chat.sphinx.contact.navigation.ContactNavigator
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.wrapper_contact.NewContact
import chat.sphinx.kotlin_response.Response
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_contact.ContactAlias
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class ContactViewModel<ARGS: NavArgs>(
    val navigator: ContactNavigator,
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    protected val contactRepository: ContactRepository,
    protected val subscriptionRepository: SubscriptionRepository,
    protected val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    val walletDataHandler: WalletDataHandler,
    val connectManager: ConnectManager,
    val imageLoader: ImageLoader<ImageView>
): SideEffectViewModel<
        Context,
        ContactSideEffect,
        ContactViewState
        >(dispatchers, ContactViewState.Idle)
{
    protected abstract val args: ARGS

    protected abstract val fromAddFriend: Boolean
    protected abstract val contactId: ContactId?


    protected var createContactJob: Job? = null



    abstract fun initContactDetails()

    private var scannerJob: Job? = null
    fun requestScanner() {
        if (scannerJob?.isActive == true) {
            return
        }

        scannerJob = viewModelScope.launch(mainImmediate) {
            val response = scannerCoordinator.submitRequest(
                ScannerRequest(
                    filter = object : ScannerFilter() {
                        override suspend fun checkData(data: String): Response<Any, String> {
                            if (data.toLightningNodePubKey() != null) {
                                return Response.Success(Any())
                            }
                            if (data.toVirtualLightningNodeAddress() != null) {
                                return Response.Success(Any())
                            }
                            return Response.Error("QR code is not a Lightning Node Public Key")
                        }
                    }
                )
            )
            if (response is Response.Success) {
                val contactInfoSideEffect : ContactSideEffect? = response.value.value.toLightningNodePubKey()?.let { lightningNodePubKey ->
                    ContactSideEffect.ContactInfo(lightningNodePubKey)
                } ?: response.value.value.toVirtualLightningNodeAddress()?.let { virtualNodeAddress ->
                    virtualNodeAddress.getPubKey()?.let { lightningNodePubKey ->
                        ContactSideEffect.ContactInfo(
                            lightningNodePubKey,
                            virtualNodeAddress.getRouteHint()
                        )
                    }
                }

                if (contactInfoSideEffect != null) {
                    submitSideEffect(contactInfoSideEffect)
                }
            }
        }
    }

    protected var saveContactJob: Job? = null
    fun saveContact(contactAlias: String?, lightningNodePubKey: String?, lightningRouteHint: String?) {
        if (saveContactJob?.isActive == true) {
            return
        }

        saveContactJob = viewModelScope.launch {

            if (contactAlias.isNullOrEmpty()) {
                submitSideEffect(ContactSideEffect.Notify.NicknameAndAddressRequired)
                return@launch
            }

            if (lightningNodePubKey.isNullOrEmpty()) {
                submitSideEffect(ContactSideEffect.Notify.InvalidLightningNodePublicKey)
                return@launch
            }

            if (!lightningRouteHint.isNullOrEmpty() && lightningRouteHint.toLightningRouteHint() == null) {
                submitSideEffect(ContactSideEffect.Notify.InvalidRouteHint)
                return@launch
            }

            createContact(
                ContactAlias(contactAlias),
                LightningNodePubKey(lightningNodePubKey),
                lightningRouteHint?.toLightningRouteHint(),
            )

//            saveContact(
//                ContactAlias(contactAlias),
//                LightningNodePubKey(lightningNodePubKey),
//                lightningRouteHint?.toLightningRouteHint()
//            )
        }
    }

    protected abstract fun createContact(
        contactAlias: ContactAlias,
        lightningNodePubKey: LightningNodePubKey,
        lightningRouteHint: LightningRouteHint?,
    )

    protected abstract fun storeContact(
        contact: NewContact
    )

    /** Sphinx V1 (likely to be removed) **/

    protected abstract fun saveContact(
        contactAlias: ContactAlias,
        lightningNodePubKey: LightningNodePubKey,
        lightningRouteHint: LightningRouteHint?
    )

    fun toQrCodeLightningNodePubKey(nodePubKey: String) {
        viewModelScope.launch(mainImmediate) {
            navigator.toQRCodeDetail(
                nodePubKey,
                app.getString(R.string.public_key),
                ""
            )
        }

    }
}
