package chat.sphinx.payment_send.ui

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard.RepositoryDashboard
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendPayment
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.payment_send.R
import chat.sphinx.payment_send.navigation.PaymentSendNavigator
import chat.sphinx.payment_send.navigation.ToPaymentSendDetail
import chat.sphinx.scanner_view_model_coordinator.request.ScannerFilter
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_lightning.NodeBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val PaymentSendFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

internal inline val PaymentSendFragmentArgs.contactId: ContactId?
    get() = if (argContactId == ToPaymentSendDetail.NULL_CONTACT_ID) {
        null
    } else {
        ContactId(argContactId)
    }

@HiltViewModel
internal class PaymentSendViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: PaymentSendNavigator,
    private val app: Application,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val repositoryDashboard: RepositoryDashboard,
    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>
): SideEffectViewModel<
        FragmentActivity,
        PaymentSendSideEffect,
        PaymentSendViewState>(dispatchers, PaymentSendViewState.Idle)
{
    private val sendPaymentBuilder = SendPayment.Builder()

    private val args: PaymentSendFragmentArgs by savedStateHandle.navArgs()

    val amountViewStateContainer: ViewStateContainer<AmountViewState> by lazy {
        ViewStateContainer(AmountViewState.Idle)
    }

    private val contactSharedFlow: SharedFlow<Contact?> = flow {
        args.contactId?.let { contactId ->
            emitAll(contactRepository.getContactById(contactId))
        } ?: run {
            emit(null)
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        repositoryDashboard.getAccountBalance()

    init {
        viewModelScope.launch(mainImmediate) {
            contactSharedFlow.collect { contact ->
                viewStateContainer.updateViewState(
                    if (contact != null) {
                        PaymentSendViewState.ChatPayment(contact)
                    } else {
                        PaymentSendViewState.KeySendPayment
                    }
                )
            }
        }
    }

    fun updateAmount(amountString: String) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(PaymentSendSideEffect.ProduceHapticFeedback)

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
                            PaymentSendSideEffect.Notify(app.getString(R.string.balance_too_low))
                        )
                    }
                }
            }
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
                            return Response.Error("QR code is not a Lightning Node Public Key")
                        }
                    },
                    showBottomView = true,
                    scannerModeLabel = app.getString(R.string.destination_key)
                )
            )
            if (response is Response.Success) {
                response.value.value.toLightningNodePubKey()?.let { destinationKey ->
                    submitSideEffect(
                        PaymentSendSideEffect.AlertConfirmPaymentSend(sendPaymentBuilder.paymentAmount, destinationKey.value) {
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
            sendPayment()
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
                        PaymentSendSideEffect.Notify(app.getString(R.string.error_processing_payment))
                    )
                    viewStateContainer.updateViewState(PaymentSendViewState.PaymentFailed)
                }
                is Response.Success -> {
                    if (sendPaymentBuilder.isKeySendPayment) {
                        val successMessage = String.format(
                            app.getString(R.string.payment_sent),
                            sendPayment?.amount ?: 0,
                            sendPayment?.destinationKey?.value ?: "Unknown"
                        )

                        submitSideEffect(
                            PaymentSendSideEffect.Notify(successMessage)
                        )
                    }
                    navigator.closeDetailScreen()
                }
            }
        }
    }

}
