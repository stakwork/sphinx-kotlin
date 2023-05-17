package chat.sphinx.chat_tribe.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState


sealed class WebViewViewState: ViewState<WebViewViewState>() {

    object Idle: WebViewViewState()

    object RequestAuthorization: WebViewViewState()

    class SendAuthorization(
        val script: String
    ): WebViewViewState()

    class SendLsat(
        val script: String,
        val error: String?
    ): WebViewViewState()

}