package chat.sphinx.payment_send.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class AmountViewState: ViewState<AmountViewState>() {
    object Idle: AmountViewState()

    class AmountUpdated(
        val amountString: String
    ): AmountViewState()
}