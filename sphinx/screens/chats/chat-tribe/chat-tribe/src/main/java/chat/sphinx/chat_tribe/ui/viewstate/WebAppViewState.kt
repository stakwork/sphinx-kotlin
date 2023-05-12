package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.wrapper_chat.AppUrl
import io.matthewnelson.concept_views.viewstate.ViewState


sealed class WebAppViewState: ViewState<WebAppViewState>() {

    object NoApp: WebAppViewState()

    sealed class AppAvailable(
        val appUrl: AppUrl
    ): WebAppViewState() {

        sealed class WebViewOpen(
            appUrl: AppUrl
        ): AppAvailable(appUrl) {

            class Loading(
                appUrl: AppUrl
            ): WebViewOpen(appUrl)

            class Loaded(
                appUrl: AppUrl
            ): WebViewOpen(appUrl)
        }

        class WebViewClosed(
            appUrl: AppUrl
        ): AppAvailable(appUrl)
    }
}