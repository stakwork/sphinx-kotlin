package chat.sphinx.tribe_badge.ui

import chat.sphinx.tribe_badge.model.TribeBadge
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class TribeBadgesViewState: ViewState<TribeBadgesViewState>() {

    object Idle : TribeBadgesViewState()

    data class TribeBadgesList(
        val tribeBadges: List<TribeBadge>
    ): TribeBadgesViewState()

}