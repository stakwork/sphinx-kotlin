package chat.sphinx.threads.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ThreadsViewState: ViewState<ThreadsViewState>() {

    object Idle: ThreadsViewState()

}
