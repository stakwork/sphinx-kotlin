package chat.sphinx.join_tribe.ui

import chat.sphinx.concept_network_query_chat.model.TribeDto
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class JoinTribeViewState: ViewState<JoinTribeViewState>() {
    object LoadingTribe : JoinTribeViewState()
    object ErrorLoadingTribe : JoinTribeViewState()

    object JoiningTribe: JoinTribeViewState()
    object ErrorJoiningTribe: JoinTribeViewState()
    object TribeJoined: JoinTribeViewState()

    data class TribeInfo(
        val tribe: TribeDto
    ): JoinTribeViewState()
}
