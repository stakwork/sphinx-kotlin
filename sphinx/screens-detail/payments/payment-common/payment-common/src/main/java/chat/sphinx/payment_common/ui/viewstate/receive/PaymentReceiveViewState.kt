package chat.sphinx.payment_common.ui.viewstate.receive

import chat.sphinx.wrapper_contact.Contact
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PaymentReceiveViewState: ViewState<PaymentReceiveViewState>() {
    object Idle: PaymentReceiveViewState()

    object KeySendPayment: PaymentReceiveViewState()

    class ChatPayment(
        val contact: Contact
    ): PaymentReceiveViewState()

    object ProcessingPayment: PaymentReceiveViewState()
    object PaymentFailed: PaymentReceiveViewState()
}