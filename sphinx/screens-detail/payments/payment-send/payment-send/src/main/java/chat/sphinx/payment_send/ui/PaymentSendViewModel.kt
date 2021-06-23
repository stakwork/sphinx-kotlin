package chat.sphinx.payment_send.ui

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
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

internal inline val PaymentSendFragmentArgs.contactId: ContactId
    get() = ContactId(argContactId)

@HiltViewModel
internal class PaymentSendViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: PaymentSendNavigator,
    private val app: Application,
    private val contactRepository: ContactRepository,
    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
): SideEffectViewModel<
        FragmentActivity,
        PaymentSendSideEffect,
        PaymentSendViewState>(dispatchers, PaymentSendViewState.Idle)
{
    private val args: PaymentSendFragmentArgs by savedStateHandle.navArgs()

    val amountViewStateContainer: ViewStateContainer<AmountViewState> by lazy {
        ViewStateContainer(AmountViewState.Idle)
    }

    private val contactSharedFlow: SharedFlow<Contact?> = flow {
        if (args.contactId.value > 0) {
            emitAll(contactRepository.getContactById(args.contactId))
        } else {
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
                }
            }
        }
    }

    fun updateAmount(amount: Int) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(PaymentSendSideEffect.ProduceHapticFeedback)

            getAccountBalance().firstOrNull()?.let { balance ->
                if (amount > 0 && amount <= balance.balance.value) {
                    amountViewStateContainer.updateViewState(AmountViewState.AmountUpdated(amount.toString()))
                } else {
                    submitSideEffect(
                        PaymentSendSideEffect.Notify(app.getString(R.string.balance_too_low))
                    )
                }
            }
        }
    }

}
