package chat.sphinx.payment_receive.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_lightning.model.RequestPayment
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.payment_common.ui.PaymentSideEffect
import chat.sphinx.payment_common.ui.PaymentViewModel
import chat.sphinx.payment_common.ui.viewstate.AmountViewState
import chat.sphinx.payment_common.ui.viewstate.receive.PaymentReceiveViewState
import chat.sphinx.payment_receive.R
import chat.sphinx.payment_receive.navigation.PaymentReceiveNavigator
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
): PaymentViewModel<PaymentReceiveFragmentArgs, PaymentReceiveViewState>(
    dispatchers,
    paymentReceiveNavigator,
    contactRepository,
    PaymentReceiveViewState.Idle
)
{
    override val args: PaymentReceiveFragmentArgs by savedStateHandle.navArgs()
    override val chatId: ChatId? = args.chatId
    override val contactId: ContactId? = args.contactId

    private val requestPaymentBuilder = RequestPayment.Builder()

    init {
        viewModelScope.launch(mainImmediate) {
            refreshViewState()
        }
    }

    private suspend fun refreshViewState() {
        contactSharedFlow.collect { contact ->
            viewStateContainer.updateViewState(
                if (contact != null) {
                    PaymentReceiveViewState.ChatPaymentRequest(contact)
                } else {
                    PaymentReceiveViewState.RequestLightningPayment
                }
            )
        }
    }

    fun requestPayment(message: String? = null) {
        viewModelScope.launch(mainImmediate) {
            requestPaymentBuilder.setChatId(args.chatId)
            requestPaymentBuilder.setContactId(args.contactId)
            requestPaymentBuilder.setMemo(message)

            val requestPayment = requestPaymentBuilder.build()

            if (requestPayment != null) {
                updateViewState(PaymentReceiveViewState.ProcessingRequest)
                val response = lightningRepository.requestPayment(requestPayment)

                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        submitSideEffect(
                            PaymentSideEffect.Notify(app.getString(R.string.failed_to_request_payment))
                        )
                        refreshViewState()
                    }
                    is Response.Success -> {
                        paymentReceiveNavigator.toQRCodeDetail(
                            response.value.value,
                            app.getString(R.string.qr_code_title),
                            String.format(app.getString(R.string.amount_n_sats), requestPayment.amount),
                            false
                        )
                    }
                }
            } else {
                submitSideEffect(PaymentSideEffect.Notify("Failed to request payment"))
            }
        }


    }

    override fun updateAmount(amountString: String) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(PaymentSideEffect.ProduceHapticFeedback)

            val updatedAmount: Int? = try {
                amountString.toInt()
            } catch (e: NumberFormatException) {
                null
            }

            requestPaymentBuilder.setAmount(updatedAmount?.toLong() ?: 0)

            when {
                updatedAmount == null -> {
                    amountViewStateContainer.updateViewState(AmountViewState.AmountUpdated(""))
                }
                updatedAmount <= MAXIMUM_RECEIVE_SAT_AMOUNT -> {
                    amountViewStateContainer.updateViewState(AmountViewState.AmountUpdated(updatedAmount.toString()))
                }
                else -> {
                    submitSideEffect(
                        PaymentSideEffect.Notify(app.getString(R.string.requested_amount_too_high))
                    )
                }
            }
        }
    }

    companion object {
        private const val MAXIMUM_RECEIVE_SAT_AMOUNT = 1_000_000
    }
}
