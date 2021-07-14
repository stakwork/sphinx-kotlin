package chat.sphinx.payment_receive.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.payment_common.ui.PaymentViewModel
import chat.sphinx.payment_common.ui.viewstate.receive.PaymentReceiveViewState
import chat.sphinx.payment_receive.navigation.PaymentReceiveNavigator
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val PaymentReceiveFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

internal inline val PaymentReceiveFragmentArgs.contactId: ContactId
    get() = ContactId(argContactId)

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
    override val contactId: ContactId = args.contactId

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
}
