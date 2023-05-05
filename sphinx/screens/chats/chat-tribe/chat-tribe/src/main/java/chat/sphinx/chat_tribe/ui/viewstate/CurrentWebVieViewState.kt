package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.wrapper_chat.AppUrl
import io.matthewnelson.concept_views.viewstate.ViewState


sealed class CurrentWebVieViewState: ViewState<CurrentWebVieViewState>() {

    object NoWebView: CurrentWebVieViewState()
    class WebViewAvailable(val appUrl: AppUrl? = null): CurrentWebVieViewState()
    object WebViewOpen: CurrentWebVieViewState()


}