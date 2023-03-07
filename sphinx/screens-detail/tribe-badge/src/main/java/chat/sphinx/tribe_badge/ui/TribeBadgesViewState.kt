package chat.sphinx.tribe_badge.ui

import chat.sphinx.tribe_badge.model.TribeBadgeHolder
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class TribeBadgesViewState: ViewState<TribeBadgesViewState>() {

    object Idle: TribeBadgesViewState()
    object Loading: TribeBadgesViewState()
    object Error: TribeBadgesViewState()

    data class TribeBadgesList(
        val tribeBadgeHolders: List<TribeBadgeHolder>
    ): TribeBadgesViewState()

}