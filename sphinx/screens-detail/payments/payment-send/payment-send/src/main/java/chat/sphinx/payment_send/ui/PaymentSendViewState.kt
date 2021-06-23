package chat.sphinx.payment_send.ui

import io.matthewnelson.concept_views.viewstate.ViewState
import chat.sphinx.wrapper_contact.Contact

internal sealed class PaymentSendViewState: ViewState<PaymentSendViewState>() {
    object Idle: PaymentSendViewState()

    object KeySendPayment: PaymentSendViewState()

    class ChatPayment(
        val contact: Contact
    ): PaymentSendViewState()
}
