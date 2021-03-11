package chat.sphinx.payment_receive.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class PaymentReceiveViewState: ViewState<PaymentReceiveViewState>() {
    object Idle: PaymentReceiveViewState()
}
