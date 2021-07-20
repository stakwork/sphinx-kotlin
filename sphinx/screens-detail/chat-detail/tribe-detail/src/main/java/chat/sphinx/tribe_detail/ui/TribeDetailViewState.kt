package chat.sphinx.tribe_detail.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class TribeDetailViewState: ViewState<TribeDetailViewState>() {
    object Idle: TribeDetailViewState()
}
