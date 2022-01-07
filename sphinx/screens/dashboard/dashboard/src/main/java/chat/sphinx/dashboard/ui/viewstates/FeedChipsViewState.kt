package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedChipsViewState: ViewState<FeedChipsViewState>() {

    object All: FeedChipsViewState()
    object Listen: FeedChipsViewState()
    object Watch: FeedChipsViewState()
    object Read: FeedChipsViewState()
//    object Play: FeedChipsViewState()
}