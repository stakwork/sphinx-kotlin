package chat.sphinx.payment_common.ui.viewstate.send

import chat.sphinx.wrapper_common.PhotoUrl
import io.matthewnelson.concept_views.viewstate.ViewState
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.SenderAlias

sealed class PaymentSendViewState: ViewState<PaymentSendViewState>() {

    object Idle: PaymentSendViewState()

    object KeySendPayment: PaymentSendViewState()

    class ChatPayment(
        val contact: Contact
    ): PaymentSendViewState()

    class TribePayment(
        val memberAlias: SenderAlias?,
        val memberColorKey: String?,
        val memberPic: PhotoUrl?,
    ): PaymentSendViewState()

    object ProcessingPayment: PaymentSendViewState()
    object PaymentFailed: PaymentSendViewState()
}
