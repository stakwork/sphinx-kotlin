package chat.sphinx.transactions.ui

import chat.sphinx.concept_network_query_message.model.TransactionDto
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class TransactionsViewState: ViewState<TransactionsViewState>() {
    abstract val list: List<TransactionDto>

    class ListMode(
        override val list: List<TransactionDto>
    ): TransactionsViewState()
}
