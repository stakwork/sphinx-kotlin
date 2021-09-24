package chat.sphinx.payment_template.ui.viewstate

import chat.sphinx.wrapper_common.payment.PaymentTemplate
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class TemplateImagesViewState: ViewState<TemplateImagesViewState>() {

    object LoadingTemplateImages: TemplateImagesViewState()

    class TemplateImages(
        val templates: List<PaymentTemplate>
    ): TemplateImagesViewState()

}