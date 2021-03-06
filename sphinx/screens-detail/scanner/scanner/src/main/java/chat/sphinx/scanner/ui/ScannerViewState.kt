package chat.sphinx.scanner.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ScannerViewState: ViewState<ScannerViewState>() {
    object Idle: ScannerViewState()
}
