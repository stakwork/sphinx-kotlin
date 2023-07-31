package chat.sphinx.threads.viewstate

import chat.sphinx.threads.model.ThreadItem
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ThreadsViewState: ViewState<ThreadsViewState>() {

    object Idle: ThreadsViewState()
    class ThreadList(val threads: List<ThreadItem>): ThreadsViewState()

}
