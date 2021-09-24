package chat.sphinx.payment_template.ui.viewstate

import chat.sphinx.payment_common.ui.viewstate.send.PaymentSendViewState
import chat.sphinx.wrapper_common.lightning.Sat
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PaymentTemplateViewState: ViewState<PaymentTemplateViewState>() {

    object Idle: PaymentTemplateViewState()
    object ProcessingPayment: PaymentTemplateViewState()
    object PaymentFailed: PaymentTemplateViewState()

}