package chat.sphinx.scanner.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ScannerViewState: ViewState<ScannerViewState>() {
    data class LayoutVisibility(
        val showBackButton: Boolean,
        val showBottomView: Boolean,
        val scannerModeLabel: String
    ): ScannerViewState()
}
