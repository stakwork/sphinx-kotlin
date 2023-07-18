package chat.sphinx.threads.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ThreadViewState: ViewState<ThreadViewState>() {

    object Idle: ThreadViewState()

}
