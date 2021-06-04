package chat.sphinx.payment_send.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class PaymentSendViewState: ViewState<PaymentSendViewState>() {
    object Idle: PaymentSendViewState()
}
