package chat.sphinx.transactions.ui

import chat.sphinx.transactions.ui.viewstate.TransactionHolderViewState
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class TransactionsViewState: ViewState<TransactionsViewState>() {

    abstract val list: List<TransactionHolderViewState>

    class ListMode(
        override val list: List<TransactionHolderViewState>,
        val loading: Boolean,
        val firstPage: Boolean,
    ): TransactionsViewState()
}
