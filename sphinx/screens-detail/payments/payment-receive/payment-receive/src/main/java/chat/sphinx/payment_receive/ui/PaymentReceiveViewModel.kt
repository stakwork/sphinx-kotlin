package chat.sphinx.payment_receive.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_lightning.model.RequestPayment
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.payment_common.ui.PaymentSideEffect
import chat.sphinx.payment_common.ui.PaymentViewModel
import chat.sphinx.payment_common.ui.viewstate.receive.PaymentReceiveViewState
import chat.sphinx.payment_receive.navigation.PaymentReceiveNavigator
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

internal inline val PaymentReceiveFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

internal inline val PaymentReceiveFragmentArgs.contactId: ContactId?
    get() = if (argContactId == ContactId.NULL_CONTACT_ID) {
        null
    } else {
        ContactId(argContactId)
    }

@HiltViewModel
internal class PaymentReceiveViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val paymentReceiveNavigator: PaymentReceiveNavigator,
    private val app: Application,
    private val contactRepository: ContactRepository,
    private val lightningRepository: LightningRepository,
    private val messageRepository: MessageRepository,
    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>
): PaymentViewModel<PaymentReceiveFragmentArgs, PaymentReceiveViewState>(
    dispatchers,
    savedStateHandle,
    paymentReceiveNavigator,
    app,
    contactRepository,
    lightningRepository,
    messageRepository,
    scannerCoordinator,
    PaymentReceiveViewState.Idle
)
{
    override val args: PaymentReceiveFragmentArgs by savedStateHandle.navArgs()
    override val chatId: ChatId? = args.chatId
    override val contactId: ContactId? = args.contactId

    private val requestPaymentBuilder = RequestPayment.Builder()

    init {
        viewModelScope.launch(mainImmediate) {
            contactSharedFlow.collect { contact ->
                viewStateContainer.updateViewState(
                    if (contact != null) {
                        PaymentReceiveViewState.ChatPayment(contact)
                    } else {
                        PaymentReceiveViewState.KeySendPayment
                    }
                )
            }
        }
    }

    fun requestPayment(message: String? = null) {
        viewModelScope.launch(mainImmediate) {
            requestPaymentBuilder.setChatId(args.chatId)
            requestPaymentBuilder.setContactId(args.contactId)
            requestPaymentBuilder.setText(message)

            val requestPayment = requestPaymentBuilder.build()
            if (requestPayment != null) {
                lightningRepository.requestPayment(requestPayment).collect { loadedResponse ->
                    @Exhaustive
                    when (loadedResponse) {
                        is LoadResponse.Loading -> {}
                        is Response.Error -> {
                            Log.e(TAG, "Error requesting payment: ", loadedResponse.cause.exception)
                            submitSideEffect(PaymentSideEffect.Notify("Failed to request payment"))
//                            updateViewState(SupportTicketViewState.Empty)
                        }
                        is Response.Success -> {
                            Log.d(TAG, "Invoice: ${loadedResponse.value.invoice}")
//                            updateViewState(PaymentReceiveViewState.KeySendPayment)
                        }
                    }
                }
            } else {
                Log.d(TAG, "Request Builder is null")
                submitSideEffect(PaymentSideEffect.Notify("Failed to request payment"))
            }
        }


    }

    override fun updateAmount(amount: Int?) {
        requestPaymentBuilder.setAmount(amount?.toLong() ?: 0)
    }

    companion object {
        private const val TAG = "PaymentReceiveViewModel"
    }
}
