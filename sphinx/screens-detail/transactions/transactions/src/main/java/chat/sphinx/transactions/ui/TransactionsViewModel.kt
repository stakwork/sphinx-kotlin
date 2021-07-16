package chat.sphinx.transactions.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_message.model.TransactionDto
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.transactions.navigation.TransactionsNavigator
import chat.sphinx.transactions.ui.viewstate.TransactionHolderViewState
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_chat.isTribeNotOwnedByAccount
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.toChatId
import chat.sphinx.wrapper_common.dashboard.toContactId
import chat.sphinx.wrapper_common.message.toMessageUUID
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.SenderAlias
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class TransactionsViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: TransactionsNavigator,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val networkQueryMessage: NetworkQueryMessage,
): BaseViewModel<TransactionsViewState>(dispatchers, TransactionsViewState.ListMode(listOf(), true))
{
    init {
        viewModelScope.launch(mainImmediate) {
            val owner: Contact = contactRepository.accountOwner.value.let { contact ->
                if (contact != null) {
                    contact
                } else {
                    var resolvedOwner: Contact? = null
                    try {
                        contactRepository.accountOwner.collect { ownerContact ->
                            if (ownerContact != null) {
                                resolvedOwner = ownerContact
                                throw Exception()
                            }
                        }
                    } catch (e: Exception) {
                    }
                    delay(25L)

                    resolvedOwner!!
                }
            }

            networkQueryMessage.getPayments().collect{ loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                        updateViewState(
                            TransactionsViewState.ListMode(listOf(), true),
                        )
                    }
                    is Response.Error -> {
                        updateViewState(
                            TransactionsViewState.ListMode(listOf(), false),
                        )
                    }
                    is Response.Success -> {
                        updateViewState(
                            TransactionsViewState.ListMode(
                                processTransactions(loadResponse.value, owner),
                                false
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun processTransactions(
        transactions: List<TransactionDto>,
        owner: Contact
    ): List<TransactionHolderViewState> {
        val transactionsHVSs = ArrayList<TransactionHolderViewState>(transactions.size)

        for (transaction in transactions) {
            var senderReceiverAlias: SenderAlias? = null
            var senderReceiverId: ContactId? = null

            if (transaction.isIncomingWithSender(owner.id)) {
                senderReceiverAlias = transaction.getSenderAlias()
                senderReceiverId = transaction.getSenderId()
            }
            else if (transaction.isOutgoingWithReceiver(owner.id)) {
                senderReceiverId = transaction.getReceiverId()
            }
            else if (transaction.isOutgoingMessageBoost(owner.id)) {
                transaction.reply_uuid?.toMessageUUID()?.let { originalMessageId ->
                    messageRepository.getMessageByUUID(originalMessageId).firstOrNull()?.let { message ->
                        senderReceiverAlias = message.senderAlias
                        senderReceiverId = message.sender
                    }
                }
            }
            else if (transaction.isPaymentInChat()) {
                transaction.getChatId()?.let { chatId ->
                    chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->
                        if (chat.isTribeNotOwnedByAccount(owner.nodePubKey) || chat.isConversation()) {
                            for (contactId in chat.contactIds) {
                                if (contactId != owner.id) {
                                    senderReceiverId = contactId
                                }
                            }
                        }
                    }
                }
            }

            val senderReceiverName: String? = when {
                (senderReceiverAlias != null) -> {
                    senderReceiverAlias!!.value
                }
                (senderReceiverId != null) -> {
                    contactRepository.getContactById(senderReceiverId!!).firstOrNull()?.let { contact ->
                        contact.alias?.value ?: null
                    }
                }
                else -> {
                    null
                }
            }

            transactionsHVSs.add(
                if (transaction.sender == owner.id.value) {
                    TransactionHolderViewState.Outgoing(
                        transaction,
                        null,
                        senderReceiverName ?: "-",
                    )
                } else {
                    TransactionHolderViewState.Incoming(
                        transaction,
                        null,
                        senderReceiverName ?: "-",
                    )
                }
            )
        }

        return transactionsHVSs
    }
}
