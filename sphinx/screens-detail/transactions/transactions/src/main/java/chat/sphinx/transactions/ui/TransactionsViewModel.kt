package chat.sphinx.transactions.ui

import android.util.Log
import androidx.lifecycle.viewModelScope
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
        // TODO V2 getPayments

//        networkQueryMessage.getPayments(
//            offset = page * itemsPerPage,
//            limit = itemsPerPage
//        ).collect { loadResponse ->
//            val firstPage = (page == 0)
//
//            @Exhaustive
//            when (loadResponse) {
//                is LoadResponse.Loading -> {
//                    updateViewState(
//                        TransactionsViewState.ListMode(currentViewState.list, true, firstPage)
//                    )
//                }
//                is Response.Error -> {
//                    updateViewState(
//                        TransactionsViewState.ListMode(listOf(), false, firstPage)
//                    )
//                }
//                is Response.Success -> {
//                    updateViewState(
//                        TransactionsViewState.ListMode(
//                            processTransactions(loadResponse.value, owner),
//                            false,
//                            firstPage
//                        )
//                    )
//                }
//            }
//        }
    }

    fun toggleFailed(
        transactionViewHolder: TransactionHolderViewState,
        position: Int
    ) {
//        (currentViewState as? TransactionsViewState.ListMode)?.let { currentVS ->
//            var list: MutableList<TransactionHolderViewState> = currentVS.list.toMutableList()
//
//            transactionViewHolder.transaction?.let { nnTransaction ->
//
//                val toggledViewHolder = when (transactionViewHolder) {
//                    is TransactionHolderViewState.Failed.Open -> {
//                        TransactionHolderViewState.Failed.Closed(
//                            nnTransaction,
//                            transactionViewHolder.invoice,
//                            transactionViewHolder.messageSenderName
//                        )
//                    }
//                    is TransactionHolderViewState.Failed.Closed -> {
//                        TransactionHolderViewState.Failed.Open(
//                            nnTransaction,
//                            transactionViewHolder.invoice,
//                            transactionViewHolder.messageSenderName
//                        )
//                    }
//                    else -> {
//                        null
//                    }
//                }
//
//                toggledViewHolder?.let {
//                    list[position] = it
//
//                    updateViewState(
//                        TransactionsViewState.ListMode(
//                            list,
//                            currentVS.loading,
//                            currentVS.firstPage
//                        )
//                    )
//                }
//            }
//        }
    }
}
