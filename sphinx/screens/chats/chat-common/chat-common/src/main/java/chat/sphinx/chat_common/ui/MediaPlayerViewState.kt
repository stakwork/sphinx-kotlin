package chat.sphinx.chat_common.ui

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class MediaPlayerViewState: ViewState<MediaPlayerViewState>() {
    object Idle: MediaPlayerViewState()
}
