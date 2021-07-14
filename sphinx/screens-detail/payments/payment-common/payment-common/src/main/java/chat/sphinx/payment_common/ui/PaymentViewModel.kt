package chat.sphinx.payment_common.ui

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavArgs
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendPayment
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.payment_common.R
import chat.sphinx.payment_common.navigation.PaymentNavigator
import chat.sphinx.payment_common.ui.viewstate.AmountViewState
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_lightning.NodeBalance
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewState
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class PaymentViewModel<ARGS: NavArgs, VS: ViewState<VS>>(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: PaymentNavigator,
    private val app: Application,
    private val contactRepository: ContactRepository,
    private val lightningRepository: LightningRepository,
    private val messageRepository: MessageRepository,
    private val scannerCoordinator: ViewModelCoordinator<ScannerRequest, ScannerResponse>,
    initialViewState: VS
): SideEffectViewModel<
        PaymentSideEffectFragment,
        PaymentSideEffect,
        VS
        >(dispatchers, initialViewState)
{
    private val sendPaymentBuilder = SendPayment.Builder()

    protected abstract val args: ARGS

    protected abstract val chatId: ChatId?
    protected abstract val contactId: ContactId?

    val amountViewStateContainer: ViewStateContainer<AmountViewState> by lazy {
        ViewStateContainer(AmountViewState.Idle)
    }

    protected val contactSharedFlow: SharedFlow<Contact?> = flow {
        contactId?.let { contactId ->
            emitAll(contactRepository.getContactById(contactId))
        } ?: run {
            emit(null)
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    protected suspend fun getAccountBalance(): StateFlow<NodeBalance?> =
        lightningRepository.getAccountBalance()

    fun updateAmount(amountString: String) {
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
                            PaymentSideEffect.Notify(app.getString(R.string.balance_too_low))
                        )
                    }
                }
            }
        }
    }

}
