package chat.sphinx.chat_tribe.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState


sealed class WebViewViewState: ViewState<WebViewViewState>() {

    object Idle: WebViewViewState()

    object RequestAuthorization: WebViewViewState()
    class ChallengeError(val error: String): WebViewViewState()

    class SendAuthorization(
        val script: String
    ): WebViewViewState()

    class SendMessage(
        val script: String,
        val error: String?
    ): WebViewViewState()


}