package chat.sphinx.payment_send.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendPayment
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.payment_common.ui.PaymentSideEffect
import chat.sphinx.payment_common.ui.PaymentViewModel
import chat.sphinx.payment_common.ui.viewstate.AmountViewState
import chat.sphinx.payment_common.ui.viewstate.send.PaymentSendViewState
import chat.sphinx.payment_send.R
import chat.sphinx.payment_send.navigation.PaymentSendNavigator
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_lightning.NodeBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val PaymentSendFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

internal inline val PaymentSendFragmentArgs.contactId: ContactId?
    get() = if (argContactId == ContactId.NULL_CONTACT_ID) {
        null
    } else {
        ContactId(argContactId)
    }

@HiltViewModel
internal class PaymentSendViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val paymentSendNavigator: PaymentSendNavigator,
    private val app: Application,
    private val contactRepository: ContactRepository,
    private val lightningRepository: LightningRepository,
    private val messageRepository: MessageRepository,
    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>
): PaymentViewModel<PaymentSendFragmentArgs, PaymentSendViewState>(
    dispatchers,
    paymentSendNavigator,
    contactRepository,
    PaymentSendViewState.Idle
)
{
    private val sendPaymentBuilder = SendPayment.Builder()

    override val args: PaymentSendFragmentArgs by savedStateHandle.navArgs()
    override val chatId: ChatId? = args.chatId
    override val contactId: ContactId? = args.contactId

    private suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        lightningRepository.getAccountBalance()

    init {
        viewModelScope.launch(mainImmediate) {
            val contact = getContactOrNull()
            viewStateContainer.updateViewState(
                if (contact != null) {
                    PaymentSendViewState.ChatPayment(contact)
                } else {
                    PaymentSendViewState.KeySendPayment
                }
            )
        }
    }

    private fun requestScanner() {
        viewModelScope.launch(mainImmediate) {
            val response = scannerCoordinator.submitRequest(
                ScannerRequest(
                    filter = object : ScannerFilter() {
                        override suspend fun checkData(data: String): Response<Any, String> {
                            if (data.toLightningNodePubKey() != null) {
                                return Response.Success(Any())
                            }
                            return Response.Error(app.getString(R.string.invalid_node_pub_key_qr_code))
                        }
                    },
                    showBottomView = true,
                    scannerModeLabel = app.getString(R.string.destination_key)
                )
            )
            if (response is Response.Success) {
                response.value.value.toLightningNodePubKey()?.let { destinationKey ->
                    submitSideEffect(
                        PaymentSideEffect.AlertConfirmPaymentSend(sendPaymentBuilder.paymentAmount, destinationKey.value) {
                            sendDirectPayment(destinationKey)
                        }
                    )
                }
            }
        }
    }

    fun sendContactPayment(message: String? = null) {
        sendPaymentBuilder.setChatId(args.chatId)
        sendPaymentBuilder.setContactId(args.contactId)
        sendPaymentBuilder.setText(message)

        if (sendPaymentBuilder.isContactPayment) {
            viewModelScope.launch {
                paymentSendNavigator.toPaymentTemplateDetail(
                    args.contactId,
                    args.chatId,
                    Sat(sendPaymentBuilder.paymentAmount),
                    message ?: "",
                )
            }
        } else {
            requestScanner()
        }
    }

    private fun sendDirectPayment(destinationKey: LightningNodePubKey) {
        sendPaymentBuilder.setDestinationKey(destinationKey)

        sendPayment()
    }

    private fun sendPayment() {
        viewStateContainer.updateViewState(PaymentSendViewState.ProcessingPayment)

        viewModelScope.launch(mainImmediate) {
            val sendPayment = sendPaymentBuilder.build()

            when (messageRepository.sendPayment(sendPayment)) {
                is Response.Error -> {
                    submitSideEffect(
                        PaymentSideEffect.Notify(app.getString(R.string.error_processing_payment))
                    )
                    viewStateContainer.updateViewState(PaymentSendViewState.PaymentFailed)
                }
                is Response.Success -> {
                    val successMessage = app.getString(
                        R.string.payment_sent,
                        sendPayment?.amount ?: 0,
                        sendPayment?.destinationKey?.value ?: "Unknown"
                    )

                    submitSideEffect(
                        PaymentSideEffect.Notify(successMessage)
                    )

                    navigator.closeDetailScreen()
                }
            }
        }
    }

    override fun updateAmount(amountString: String) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(PaymentSideEffect.ProduceHapticFeedback)

            getAccountBalance().firstOrNull()?.let { balance ->
                val updatedAmount: Int? = try {
                    amountString.toInt()
                } catch (e: NumberFormatException) {
                    null
                }

                sendPaymentBuilder.setAmount(updatedAmount?.toLong() ?: 0)

                when {
                    updatedAmount == null -> {
                        amountViewStateContainer.updateViewState(AmountViewState.AmountUpdated(""))
                    }
                    updatedAmount <= balance.balance.value -> {
                        amountViewStateContainer.updateViewState(AmountViewState.AmountUpdated(updatedAmount.toString()))
                    }
                    else -> {
                        submitSideEffect(
                            PaymentSideEffect.Notify(app.getString(chat.sphinx.payment_common.R.string.balance_too_low))
                        )
                    }
                }
            }
        }
    }

}
