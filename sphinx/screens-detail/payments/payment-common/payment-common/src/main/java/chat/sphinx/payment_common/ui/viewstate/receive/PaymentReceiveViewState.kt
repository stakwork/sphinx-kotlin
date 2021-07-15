package chat.sphinx.payment_common.ui.viewstate.receive

import chat.sphinx.wrapper_contact.Contact
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PaymentReceiveViewState: ViewState<PaymentReceiveViewState>() {
    object Idle: PaymentReceiveViewState()

    object RequestLightningPayment: PaymentReceiveViewState()

    class ChatPaymentRequest(
        val contact: Contact
    ): PaymentReceiveViewState()

    object ProcessingRequest: PaymentReceiveViewState()
    object RequestFailed: PaymentReceiveViewState()
}