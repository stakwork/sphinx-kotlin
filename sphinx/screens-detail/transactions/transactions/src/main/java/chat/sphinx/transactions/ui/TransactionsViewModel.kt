package chat.sphinx.transactions.ui

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
import chat.sphinx.wrapper_chat.isTribeNotOwnedByAccount
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_common.message.toMessageUUID
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.SenderAlias
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
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
        ).collect { loadResponse ->
            val firstPage = (page == 0)

            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {
                    updateViewState(
                        TransactionsViewState.ListMode(currentViewState.list, true, firstPage)
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

        var chatsIdsMap: MutableMap<ChatId, ArrayList<Long>> = LinkedHashMap(transactions.size)
        var originalMessageUUIDsMap: MutableMap<MessageUUID, Long> = LinkedHashMap(transactions.size)

        var contactIdsMap: MutableMap<Long, ContactId> = LinkedHashMap(transactions.size)
        var contactAliasMap: MutableMap<Long, SenderAlias> = LinkedHashMap(transactions.size)

        for (transaction in transactions) {
            when {
                transaction.isIncomingWithSender(owner.id) -> {
                    transaction.getSenderId()?.let { senderId ->
                        contactIdsMap[transaction.id] = senderId
                    }
                    transaction.getSenderAlias()?.let { senderAlias ->
                        contactAliasMap[transaction.id] = senderAlias
                    }
                }
                transaction.isOutgoingWithReceiver(owner.id) -> {
                    transaction.getSenderId()?.let { senderId ->
                        contactIdsMap[transaction.id] = senderId
                    }
                }
                transaction.isOutgoingMessageBoost(owner.id) -> {
                    transaction.reply_uuid?.toMessageUUID()?.let { originalMessageUUID ->
                        originalMessageUUIDsMap[originalMessageUUID] = transaction.id
                    }
                }
                transaction.isPaymentInChat() -> {
                    transaction.getChatId()?.let { chatId ->
                        if (chatsIdsMap[chatId] == null) {
                            chatsIdsMap[chatId] = ArrayList(0)
                        }
                        chatsIdsMap[chatId]?.add(transaction.id)
                    }
                }
            }
        }

        val chatIds = chatsIdsMap.keys.map { it }
        chatRepository.getAllChatsByIds(chatIds).let { response ->
            response.forEach { chat ->
                if (
                    (chat.isTribeNotOwnedByAccount(owner.nodePubKey) || chat.isConversation()) &&
                    chat.contactIds.size == 2
                ) {
                    chatsIdsMap[chat.id]?.let { transactionIds ->
                        for (transactionId in transactionIds) {
                            contactIdsMap[transactionId] = chat.contactIds[1]
                        }
                    }
                }
            }
        }

        val originalMessageUUIDs = originalMessageUUIDsMap.keys.map { it }
        messageRepository.getAllMessagesByUUID(originalMessageUUIDs).let { response ->
            response.forEach { message ->
                originalMessageUUIDsMap[message.uuid]?.let { transactionId ->
                    contactIdsMap[transactionId] = message.sender

                    message.senderAlias?.let { senderAlias ->
                        contactAliasMap[transactionId] = senderAlias
                    }
                }
            }
        }

        val contactsMap: MutableMap<Long, Contact> = LinkedHashMap(transactions.size)
        val contactIds = contactIdsMap.values.map { it }

        contactRepository.getAllContactsByIds(contactIds).let { response ->
            response.forEach { contact ->
                contactsMap[contact.id.value] = contact
            }
        }

        val transactionsHVSs: MutableList<TransactionHolderViewState> = currentViewState.list.toMutableList()

        if (transactionsHVSs.lastOrNull() is TransactionHolderViewState.Loader) {
            transactionsHVSs.removeLast()
        }

        for (transaction in transactions) {
            val senderId = contactIdsMap[transaction.id]
            val senderAlias: String? = contactAliasMap[transaction.id]?.value ?: contactsMap[senderId?.value]?.alias?.value

            if (transaction.isOutgoingPayment(owner.id)) {
                transactionsHVSs.add(
                    TransactionHolderViewState.Outgoing(
                        transaction,
                        null,
                        senderAlias ?: "-",
                    )
                )
            }
            if (transaction.isIncomingPayment(owner.id)) {
                transactionsHVSs.add(
                    TransactionHolderViewState.Incoming(
                        transaction,
                        null,
                        senderAlias ?: "-",
                    )
                )
            }
            if (transaction.isFailedPayment()) {
                transactionsHVSs.add(
                    TransactionHolderViewState.Failed.Closed(
                        transaction,
                        null,
                        senderAlias ?: "-",
                    )
                )
            }
        }

        if (transactions.size == itemsPerPage) {
            transactionsHVSs.add(
                TransactionHolderViewState.Loader()
            )
        }

        return transactionsHVSs
    }

    fun toggleFailed(
        transactionViewHolder: TransactionHolderViewState,
        position: Int
    ) {
        (currentViewState as? TransactionsViewState.ListMode)?.let { currentVS ->
            var list: MutableList<TransactionHolderViewState> = currentVS.list.toMutableList()

            transactionViewHolder.transaction?.let { nnTransaction ->

                val toggledViewHolder = when (transactionViewHolder) {
                    is TransactionHolderViewState.Failed.Open -> {
                        TransactionHolderViewState.Failed.Closed(
                            nnTransaction,
                            transactionViewHolder.invoice,
                            transactionViewHolder.messageSenderName
                        )
                    }
                    is TransactionHolderViewState.Failed.Closed -> {
                        TransactionHolderViewState.Failed.Open(
                            nnTransaction,
                            transactionViewHolder.invoice,
                            transactionViewHolder.messageSenderName
                        )
                    }
                    else -> {
                        null
                    }
                }

                toggledViewHolder?.let {
                    list[position] = it

                    updateViewState(
                        TransactionsViewState.ListMode(
                            list,
                            currentVS.loading,
                            currentVS.firstPage
                        )
                    )
                }
            }
        }
    }
}
