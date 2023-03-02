package chat.sphinx.tribe_badge.ui

import chat.sphinx.tribe_badge.model.TribeBadgeListHolder
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class TribeBadgesViewState: ViewState<TribeBadgesViewState>() {

    object Idle: TribeBadgesViewState()
    object Loading: TribeBadgesViewState()
    object Close: TribeBadgesViewState()

    data class TribeBadgesList(
        val tribeBadgeListHolders: List<TribeBadgeListHolder>
    ): TribeBadgesViewState()

}