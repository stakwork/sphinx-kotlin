package chat.sphinx.send_attachment.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class SendAttachmentViewState: ViewState<SendAttachmentViewState>() {
    data class LayoutVisibility(
        val paymentAndInvoiceEnabled: Boolean,
    ): SendAttachmentViewState()
}
