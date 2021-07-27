package chat.sphinx.join_tribe.ui

import chat.sphinx.concept_network_query_chat.model.TribeDto
import io.matthewnelson.concept_views.viewstate.ViewState
import java.io.File

internal sealed class JoinTribeViewState: ViewState<JoinTribeViewState>() {
    object LoadingTribe : JoinTribeViewState()
    object ErrorLoadingTribe : JoinTribeViewState()

    data class TribeLoaded(
        val tribe: TribeDto
    ): JoinTribeViewState()

    class TribeProfileImageUpdated(
        val imageFile: File
    ): JoinTribeViewState()

    object JoiningTribe: JoinTribeViewState()
    object ErrorJoiningTribe: JoinTribeViewState()
    object TribeJoined: JoinTribeViewState()
}
