package chat.sphinx.payment_common.ui.viewstate.send

import io.matthewnelson.concept_views.viewstate.ViewState
import chat.sphinx.wrapper_contact.Contact

sealed class PaymentSendViewState: ViewState<PaymentSendViewState>() {
    object Idle: PaymentSendViewState()

    object KeySendPayment: PaymentSendViewState()

    class ChatPayment(
        val contact: Contact
    ): PaymentSendViewState()

    object ProcessingPayment: PaymentSendViewState()
    object PaymentFailed: PaymentSendViewState()
}
