package chat.sphinx.tribe_badge.ui

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class TribeBadgesViewState: ViewState<TribeBadgesViewState>() {

    object Idle : TribeBadgesViewState()

}