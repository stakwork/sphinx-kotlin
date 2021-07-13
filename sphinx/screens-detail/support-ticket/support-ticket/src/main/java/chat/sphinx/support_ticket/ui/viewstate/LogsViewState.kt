package chat.sphinx.support_ticket.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class LogsViewState: ViewState<LogsViewState>() {
    object Empty: LogsViewState()
    data class Fetched(val logs: String): LogsViewState()
}