package chat.sphinx.transactions.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class TransactionsViewState: ViewState<TransactionsViewState>() {
    object Idle: TransactionsViewState()
}
