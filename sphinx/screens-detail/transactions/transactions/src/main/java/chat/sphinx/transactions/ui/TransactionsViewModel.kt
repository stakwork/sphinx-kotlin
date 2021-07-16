package chat.sphinx.transactions.ui

import android.content.ContentValues.TAG
import android.util.Log
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
import chat.sphinx.wrapper_chat.isTribe
import chat.sphinx.wrapper_chat.isTribeNotOwnedByAccount
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.toChatId
import chat.sphinx.wrapper_common.dashboard.toContactId
import chat.sphinx.wrapper_common.lightning.toLightningPaymentHash
import chat.sphinx.wrapper_common.lightning.toLightningPaymentRequest
import chat.sphinx.wrapper_common.message.toMessageUUID
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.SenderAlias
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
            var senderReceiverAlias: String? = null
            var senderReceiverId: Long? = null

            if (transaction.amount == 125.toLong()) {
                Log.d(TAG, "TEST")
            }


            if (transaction.sender != owner.id.value) {
                senderReceiverId = transaction.sender
            }
            if (transaction.sender == owner.id.value && transaction.receiver != null) {
                senderReceiverId = transaction.receiver
            }
            if (transaction.reply_uuid != null) {
                transaction.reply_uuid?.toMessageUUID()?.let { originalMessageId ->
                    messageRepository.getMessageByUUID(originalMessageId).firstOrNull()?.let { message ->
                        senderReceiverAlias = message.senderAlias?.value
                        senderReceiverId = message.sender.value
                    }
                }
            }
            if (transaction.payment_hash != null) {
                transaction.payment_hash?.toLightningPaymentHash()?.let { paymentHash ->
                    messageRepository.getInvoiceBy(paymentHash)?.firstOrNull()?.let { message ->
                        if (message.sender == owner.id) {
                            senderReceiverId = message.receiver?.value
                        } else {
                            senderReceiverAlias = message.senderAlias?.value
                            senderReceiverId =message.sender.value
                        }
                    }
                }
            }
            if (transaction.payment_request != null) {
                transaction.payment_request?.toLightningPaymentRequest()?.let { paymentRequest ->
                    messageRepository.getInvoiceBy(paymentRequest)?.firstOrNull()?.let { message ->
                        if (message.sender == owner.id) {
                            senderReceiverId = message.receiver?.value
                        } else {
                            senderReceiverAlias = message.senderAlias?.value
                            senderReceiverId = message.sender.value
                        }
                    }
                }
            }
            if (transaction.chat_id != null) {
                transaction.chat_id?.toChatId()?.let { chatId ->
                    chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->
                        if (chat.isTribeNotOwnedByAccount(owner.nodePubKey) || chat.isConversation()) {
                            for (cId in chat.contactIds) {
                                if (cId != owner.id) {
                                    senderReceiverId = cId.value
                                }
                            }
                        }
                    }
                }
            }

            val senderReceiverName: String? = when {
                senderReceiverAlias != null -> {
                    senderReceiverAlias
                }
                senderReceiverId != null -> {
                    senderReceiverId?.toContactId()?.let { contactId ->
                        contactRepository.getContactById(contactId).firstOrNull()?.let { contact ->
                            contact.alias?.value ?: null
                        }
                    }
                } else -> {
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
