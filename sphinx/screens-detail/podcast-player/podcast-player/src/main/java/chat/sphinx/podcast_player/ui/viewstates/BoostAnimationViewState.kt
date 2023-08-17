package chat.sphinx.podcast_player.ui.viewstates

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.Sat
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class BoostAnimationViewState: ViewState<BoostAnimationViewState>() {
    object Idle: BoostAnimationViewState()

    class BoosAnimationInfo(
        val photoUrl: PhotoUrl?,
        val amount: Sat?
    ): BoostAnimationViewState()

}