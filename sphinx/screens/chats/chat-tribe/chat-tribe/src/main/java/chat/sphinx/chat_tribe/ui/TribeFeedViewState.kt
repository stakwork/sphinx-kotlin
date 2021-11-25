package chat.sphinx.chat_tribe.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class TribeFeedViewState: ViewState<TribeFeedViewState>() {
    object Idle: TribeFeedViewState()
}