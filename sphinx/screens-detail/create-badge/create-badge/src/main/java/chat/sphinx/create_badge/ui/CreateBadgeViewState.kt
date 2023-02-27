package chat.sphinx.create_badge.ui

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class CreateBadgeViewState: ViewState<CreateBadgeViewState>() {

    object Idle : CreateBadgeViewState()

}