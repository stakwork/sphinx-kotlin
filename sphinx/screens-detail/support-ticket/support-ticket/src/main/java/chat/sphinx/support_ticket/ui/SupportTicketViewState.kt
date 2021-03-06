package chat.sphinx.support_ticket.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class SupportTicketViewState: ViewState<SupportTicketViewState>() {
    object Idle: SupportTicketViewState()
}
