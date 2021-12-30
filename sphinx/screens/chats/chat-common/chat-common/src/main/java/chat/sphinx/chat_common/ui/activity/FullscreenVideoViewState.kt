package chat.sphinx.chat_common.ui.activity

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed  class FullscreenVideoViewState(
    val videoDimensions: Pair<Int, Int>,
) : ViewState<FullscreenVideoViewState>() {
    object Idle : FullscreenVideoViewState(
        Pair(0,0),
    )

    class MetaDataLoaded(
        videoDimensions: Pair<Int, Int>,
    ): FullscreenVideoViewState(
        videoDimensions,
    )
}