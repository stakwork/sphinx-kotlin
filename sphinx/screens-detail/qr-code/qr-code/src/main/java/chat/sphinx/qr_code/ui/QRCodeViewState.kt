package chat.sphinx.qr_code.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class QRCodeViewState: ViewState<QRCodeViewState>() {
    data class LayoutVisibility(
        val showBackButton: Boolean,
    ): QRCodeViewState()
}
