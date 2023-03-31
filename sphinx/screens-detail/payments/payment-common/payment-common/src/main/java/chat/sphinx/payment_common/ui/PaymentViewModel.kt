package chat.sphinx.payment_common.ui

import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavArgs
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendPayment
import chat.sphinx.payment_common.navigation.PaymentNavigator
import chat.sphinx.payment_common.ui.viewstate.AmountViewState
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.Message
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewState
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.*

abstract class PaymentViewModel<ARGS: NavArgs, VS: ViewState<VS>>(
    dispatchers: CoroutineDispatchers,
    val navigator: PaymentNavigator,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    initialViewState: VS
): SideEffectViewModel<
        FragmentActivity,
        PaymentSideEffect,
        VS
        >(dispatchers, initialViewState)
{
    private val sendPaymentBuilder = SendPayment.Builder()

    protected abstract val args: ARGS

    protected abstract val chatId: ChatId?
    protected abstract val contactId: ContactId?
    protected abstract val messageUUID: MessageUUID?
    protected abstract val lightningNodePubKey: LightningNodePubKey?
    protected abstract val routeHint: LightningRouteHint?

    val amountViewStateContainer: ViewStateContainer<AmountViewState> by lazy {
        ViewStateContainer(AmountViewState.Idle)
    }

    protected suspend fun getContactOrNull(): Contact? {
        return contactId?.let { id -> contactRepository.getContactById(id).firstOrNull() }
    }

    protected suspend fun getChatOrNull(): Chat? {
        return chatId?.let { id -> chatRepository.getChatById(id).firstOrNull() }
    }

    protected suspend fun getMessageOrNull(): Message? {
        return messageUUID?.let { uuid -> messageRepository.getMessageByUUID(uuid).firstOrNull() }
    }

    abstract fun updateAmount(amountString: String)

}
