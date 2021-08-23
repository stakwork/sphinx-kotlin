package chat.sphinx.contact.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavArgs
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_contact.model.ContactForm
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.contact.R
import chat.sphinx.contact.navigation.ContactNavigator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.*
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch

abstract class ContactViewModel<ARGS: NavArgs> (
    val navigator: ContactNavigator,
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    protected val contactRepository: ContactRepository,
    protected val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>
): SideEffectViewModel<
        Context,
        ContactSideEffect,
        ContactViewState
        >(dispatchers, ContactViewState.Idle)
{
    protected abstract val args: ARGS

    protected abstract val fromAddFriend: Boolean
    protected abstract val contactId: ContactId?

    abstract fun initContactDetails()

    fun requestScanner() {
        viewModelScope.launch(mainImmediate) {
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

    fun saveContact(contactFormBuilder: ContactForm.Builder) {
        viewModelScope.launch {
            if (!contactFormBuilder.hasContactAlias) {
                submitSideEffect(ContactSideEffect.Notify.NicknameAndAddressRequired)
                return@launch
            }

            if (!contactFormBuilder.hasLightningNodePubKey) {
                submitSideEffect(ContactSideEffect.Notify.InvalidLightningNodePublicKey)
                return@launch
            }

            if (!contactFormBuilder.hasValidLightningRouteHint) {
                submitSideEffect(ContactSideEffect.Notify.InvalidRouteHint)
                return@launch
            }

            contactFormBuilder.build()?.let { contactForm ->
                saveContact(contactForm)
            }
        }
    }

    protected abstract fun saveContact(contactForm: ContactForm)

    fun isFromAddFriend(): Boolean {
        return fromAddFriend
    }

    fun isExistingContact(): Boolean {
        return contactId != null
    }

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
