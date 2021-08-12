package chat.sphinx.payment_common.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class AmountViewState: ViewState<AmountViewState>() {
    object Idle: AmountViewState()

    class AmountUpdated(
        val amountString: String
    ): AmountViewState()
}