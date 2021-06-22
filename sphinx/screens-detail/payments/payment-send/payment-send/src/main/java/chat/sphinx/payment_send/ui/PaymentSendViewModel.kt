package chat.sphinx.payment_send.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.payment_send.navigation.PaymentSendNavigator
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_contact.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
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
    protected val contactRepository: ContactRepository,
): BaseViewModel<PaymentSendViewState>(
    dispatchers,
    PaymentSendViewState.Idle
)
{
    private val args: PaymentSendFragmentArgs by savedStateHandle.navArgs()

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

    init {
        viewModelScope.launch(mainImmediate) {
            contactSharedFlow.collect { contact ->
                contact?.let { contact ->
                    viewStateContainer.updateViewState(PaymentSendViewState.SendingChatPayment(contact))
                } ?: run {

                }
            }
        }
    }

}
