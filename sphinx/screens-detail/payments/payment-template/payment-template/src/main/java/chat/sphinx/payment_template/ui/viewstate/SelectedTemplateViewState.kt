package chat.sphinx.payment_template.ui.viewstate

import chat.sphinx.wrapper_common.payment.PaymentTemplate
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class SelectedTemplateViewState: ViewState<SelectedTemplateViewState>() {

    object Idle: SelectedTemplateViewState()

    class SelectedTemplate(
        val template: PaymentTemplate
    ): SelectedTemplateViewState()

}