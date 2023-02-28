package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.chat_tribe.ui.BadgesListViewModel
import chat.sphinx.concept_network_query_people.model.BadgeDto
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class BadgesListViewState(): ViewState<BadgesListViewState>() {
    object Idle: BadgesListViewState()

    class BadgesLoaded(
        val badges: List<BadgeDto>
    ): BadgesListViewState()
}

