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
import chat.sphinx.wrapper_common.dashboard.ContactId
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
): BaseViewModel<TransactionsViewState>(
    dispatchers,
    TransactionsViewState.ListMode(
        listOf(),
        loading = true,
        firstPage = true
    )
)
{
    private var page: Int = 0
    private var loading: Boolean = false
    private val itemsPerPage: Int = 50

    private suspend fun getOwnerContact(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
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
    }

    init {
        viewModelScope.launch(mainImmediate) {
            loadTransactions(
                getOwnerContact()
            )
        }
    }

    suspend fun loadMoreTransactions() {
        if (loading) {
            return
        }

        loading = true
        page += 1

        loadTransactions(
            getOwnerContact()
        )

        loading = false
    }

    private suspend fun loadTransactions(
        owner: Contact
    ) {
        networkQueryMessage.getPayments(
            offset = page * itemsPerPage,
            limit = itemsPerPage
        ).collect{ loadResponse ->
            val firstPage = (page == 0)

            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {
                    updateViewState(
                        TransactionsViewState.ListMode(listOf(), true, firstPage)
                    )
                }
                is Response.Error -> {
                    updateViewState(
                        TransactionsViewState.ListMode(listOf(), false, firstPage)
                    )
                }
                is Response.Success -> {
                    updateViewState(
                        TransactionsViewState.ListMode(
                            processTransactions(loadResponse.value, owner),
                            false,
                            firstPage
                        )
                    )
                }
            }
        }
    }

    private suspend fun processTransactions(
        transactions: List<TransactionDto>,
        owner: Contact
    ): List<TransactionHolderViewState> {

        val contactIdsMap: MutableMap<Long, ContactId> = LinkedHashMap(transactions.size)
        val contactAliasMap: MutableMap<Long, SenderAlias> = LinkedHashMap(transactions.size)

        for (transaction in transactions) {
            var senderReceiverAlias: SenderAlias? = null
            var senderReceiverId: ContactId? = null

            when {
                transaction.isIncomingWithSender(owner.id) -> {
                    senderReceiverAlias = transaction.getSenderAlias()
                    senderReceiverId = transaction.getSenderId()
                }
                transaction.isOutgoingWithReceiver(owner.id) -> {
                    senderReceiverId = transaction.getReceiverId()
                }
                transaction.isOutgoingMessageBoost(owner.id) -> {
                    transaction.reply_uuid?.toMessageUUID()?.let { originalMessageId ->
                        messageRepository.getMessageByUUID(originalMessageId).firstOrNull()?.let { message ->
                            senderReceiverAlias = message.senderAlias
                            senderReceiverId = message.sender
                        }
                    }
                }
                transaction.isPaymentInChat() -> {
                    transaction.getChatId()?.let { chatId ->
                        chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->
                            if (chat.isTribeNotOwnedByAccount(owner.nodePubKey) || chat.isConversation()) {
                                senderReceiverId = if (chat.contactIds.size > 1) chat.contactIds[1] else null
                            }
                        }
                    }
                }
            }

            senderReceiverId?.let { srID ->
                contactIdsMap[transaction.id] = srID
            }
            senderReceiverAlias?.let { srAlias ->
                contactAliasMap[transaction.id] = srAlias
            }
        }

        val contactsMap: MutableMap<Long, Contact> = LinkedHashMap(transactions.size)
        val contactIds = contactIdsMap.values.map { it }

        contactRepository.getAllContactsByIds(contactIds).let { response ->
            response.forEach { contact ->
                contactsMap[contact.id.value] = contact
            }
        }

        val transactionsHVSs = ArrayList<TransactionHolderViewState>(transactions.size)

        for (transaction in transactions) {
            val senderId = contactIdsMap[transaction.id]
            val senderAlias: String? = contactAliasMap[transaction.id]?.value ?: contactsMap[senderId?.value]?.alias?.value

            transactionsHVSs.add(
                if (transaction.sender == owner.id.value) {
                    TransactionHolderViewState.Outgoing(
                        transaction,
                        null,
                        senderAlias ?: "-",
                    )
                } else {
                    TransactionHolderViewState.Incoming(
                        transaction,
                        null,
                        senderAlias ?: "-",
                    )
                }
            )
        }

        if (transactions.size == itemsPerPage) {
            transactionsHVSs.add(
                TransactionHolderViewState.Loader()
            )
        }

        return transactionsHVSs
    }
}
