package chat.sphinx.payment_send.ui

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.payment_send.R
import chat.sphinx.payment_send.navigation.PaymentSendNavigator
import chat.sphinx.wrapper_common.dashboard.ContactId
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
import chat.sphinx.concept_repository_message.SendPayment
import chat.sphinx.wrapper_common.dashboard.ChatId

internal inline val PaymentSendFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

internal inline val PaymentSendFragmentArgs.contactId: ContactId?
    get() = if (argContactId == ContactId.NULL_CONTACT_ID.toLong()) {
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
    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
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

    suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        repositoryDashboard.getAccountBalance()

    init {
        viewModelScope.launch(mainImmediate) {
            contactSharedFlow.collect { contact ->
                contact?.let { contact ->
                    viewStateContainer.updateViewState(PaymentSendViewState.ChatPayment(contact))
                } ?: run {
                    viewStateContainer.updateViewState(PaymentSendViewState.KeySendPayment)
                }
            }
        }
    }

    fun updateAmount(amountString: String) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(PaymentSendSideEffect.ProduceHapticFeedback)

            getAccountBalance().firstOrNull()?.let { balance ->
                var updatedAmount: Int? = try {
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

    private fun goToScanner() {

    }

    fun sendChatPayment(message: String? = null) {
        if (args.contactId != null) {
            sendPaymentBuilder.setChatId(args.chatId)
            sendPaymentBuilder.setContactId(args.contactId)
            sendPaymentBuilder.setText(message)

            messageRepository.sendPayment(sendPaymentBuilder.build())
        } else {
            goToScanner()
        }
    }

    private fun sendDirectPayment(destinationKey: String) {
        sendPaymentBuilder.setDestinationKey(destinationKey)

        messageRepository.sendPayment(sendPaymentBuilder.build())
    }

}
