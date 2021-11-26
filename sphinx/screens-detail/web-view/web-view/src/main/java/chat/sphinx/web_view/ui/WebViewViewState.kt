package chat.sphinx.web_view.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class WebViewViewState: ViewState<WebViewViewState>() {
    object Idle: WebViewViewState()
}
